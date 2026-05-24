package com.hospital.ai.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.ai.embedding.EmbeddingService;
import com.hospital.healthrecord.domain.HealthRecord;
import com.hospital.healthrecord.repository.HealthRecordRepository;
import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.repository.MedicalRecordRepository;
import com.hospital.visit.domain.VisitRecord;
import com.hospital.visit.repository.VisitRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates the embedding pipeline for one source row at a time:
 * load entity → chunk → embed → upsert into {@code patient_knowledge_chunk}.
 *
 * <p>Failures are captured in {@code ai_dead_letter} (per source row, with a
 * retry counter) so a transient embedding outage doesn't lose work. Re-runs
 * are safe thanks to the {@code (source_type, source_id, field_key)} unique
 * constraint — we use {@code ON CONFLICT DO UPDATE} to refresh the
 * embedding when the source field changes.</p>
 *
 * <p>Each public {@code ingest*} method runs in its OWN transaction
 * ({@code REQUIRES_NEW}). The {@code @TransactionalEventListener(AFTER_COMMIT)}
 * already runs outside the publishing transaction, but making it explicit
 * keeps the backfill admin endpoint safe to call from a controller too.</p>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);
    private static final int MAX_RETRIES = 5;

    private final MedicalRecordRepository medicalRecordRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordChunker medicalRecordChunker;
    private final HealthRecordChunker healthRecordChunker;
    private final VisitRecordChunker visitRecordChunker;
    private final EmbeddingService embeddingService;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public KnowledgeIngestionService(MedicalRecordRepository medicalRecordRepository,
                                     HealthRecordRepository healthRecordRepository,
                                     VisitRepository visitRepository,
                                     MedicalRecordChunker medicalRecordChunker,
                                     HealthRecordChunker healthRecordChunker,
                                     VisitRecordChunker visitRecordChunker,
                                     EmbeddingService embeddingService,
                                     JdbcClient jdbcClient,
                                     ObjectMapper objectMapper) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.visitRepository = visitRepository;
        this.medicalRecordChunker = medicalRecordChunker;
        this.healthRecordChunker = healthRecordChunker;
        this.visitRecordChunker = visitRecordChunker;
        this.embeddingService = embeddingService;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionOutcome ingestMedicalRecord(Long id) {
        MedicalRecord r = medicalRecordRepository.findById(id).orElse(null);
        if (r == null) return IngestionOutcome.notFound();
        return ingestChunks(KnowledgeChunk.SOURCE_MEDICAL_RECORD, id, r.getPatientId(),
                medicalRecordChunker.chunk(r));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionOutcome ingestHealthRecord(Long id) {
        HealthRecord r = healthRecordRepository.findById(id).orElse(null);
        if (r == null) return IngestionOutcome.notFound();
        return ingestChunks(KnowledgeChunk.SOURCE_HEALTH_RECORD, id, r.getPatientId(),
                healthRecordChunker.chunk(r));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionOutcome ingestVisitRecord(Long id) {
        VisitRecord r = visitRepository.findById(id).orElse(null);
        if (r == null) return IngestionOutcome.notFound();
        return ingestChunks(KnowledgeChunk.SOURCE_VISIT, id, r.getPatientId(),
                visitRecordChunker.chunk(r));
    }

    // --- shared embedding + upsert path ------------------------------------

    IngestionOutcome ingestChunks(String sourceType, Long sourceId, Long patientId,
                                  List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return IngestionOutcome.skipped();
        }
        if (patientId == null) {
            log.warn("Skipping ingestion for {}#{}: source row has no patient_id", sourceType, sourceId);
            recordDeadLetter("embedding", sourceType, sourceId, null,
                    Map.of("reason", "missing_patient_id"),
                    "source row has no patient_id");
            return IngestionOutcome.skipped();
        }

        List<String> texts = chunks.stream().map(KnowledgeChunk::chunkText).toList();
        EmbeddingService.EmbeddingBatchResult embedded;
        try {
            embedded = embeddingService.embed(texts);
        } catch (RuntimeException ex) {
            log.warn("Embedding failed for {}#{}: {}", sourceType, sourceId, ex.toString());
            recordDeadLetter("embedding", sourceType, sourceId, patientId,
                    Map.of("chunk_count", chunks.size()),
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            throw ex;
        }

        List<float[]> vectors = embedded.vectors();
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch: chunks="
                    + chunks.size() + " vectors=" + vectors.size());
        }

        for (int i = 0; i < chunks.size(); i++) {
            upsert(chunks.get(i), vectors.get(i));
        }
        return IngestionOutcome.processed(chunks.size(), embedded.totalTokens());
    }

    private void upsert(KnowledgeChunk chunk, float[] vector) {
        String metadataJson = toJson(chunk.metadata());
        String vectorLiteral = toPgVectorLiteral(vector);
        jdbcClient.sql("""
                INSERT INTO patient_knowledge_chunk
                    (patient_id, source_type, source_id, field_key, chunk_text, embedding, metadata)
                VALUES (:pid, :stype, :sid, :fkey, :text, CAST(:vec AS vector), CAST(:meta AS jsonb))
                ON CONFLICT (source_type, source_id, field_key)
                DO UPDATE SET
                    chunk_text = EXCLUDED.chunk_text,
                    embedding  = EXCLUDED.embedding,
                    metadata   = EXCLUDED.metadata,
                    patient_id = EXCLUDED.patient_id
                """)
                .param("pid", chunk.patientId())
                .param("stype", chunk.sourceType())
                .param("sid", chunk.sourceId())
                .param("fkey", chunk.fieldKey())
                .param("text", chunk.chunkText())
                .param("vec", vectorLiteral)
                .param("meta", metadataJson)
                .update();
    }

    /** pgvector wire format: "[0.1,0.2,...]". */
    public static String toPgVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 12);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    String toJson(Map<String, Object> m) {
        if (m == null || m.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise chunk metadata: {}", ex.toString());
            return "{}";
        }
    }

    // --- dead-letter -------------------------------------------------------

    void recordDeadLetter(String kind, String sourceType, Long sourceId, Long patientId,
                          Map<String, Object> payload, String error) {
        try {
            int updated = jdbcClient.sql("""
                    UPDATE ai_dead_letter
                       SET retry_count = retry_count + 1,
                           last_attempt_at = NOW(),
                           error_msg = :err,
                           payload = CAST(:payload AS jsonb)
                     WHERE kind = :kind AND source_type = :stype AND source_id = :sid
                       AND resolved_at IS NULL
                    """)
                    .param("err", truncate(error, 1000))
                    .param("payload", toJson(payload))
                    .param("kind", kind)
                    .param("stype", sourceType)
                    .param("sid", sourceId)
                    .update();
            if (updated == 0) {
                jdbcClient.sql("""
                        INSERT INTO ai_dead_letter
                            (kind, source_type, source_id, patient_id, payload, error_msg)
                        VALUES (:kind, :stype, :sid, :pid, CAST(:payload AS jsonb), :err)
                        """)
                        .param("kind", kind)
                        .param("stype", sourceType)
                        .param("sid", sourceId)
                        .param("pid", patientId)
                        .param("payload", toJson(payload))
                        .param("err", truncate(error, 1000))
                        .update();
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to write ai_dead_letter row: {}", ex.toString());
        }
    }

    /**
     * @return true when the source row has previously failed {@value #MAX_RETRIES}
     *         times and should be left alone. KnowledgeBackfillJob consults this
     *         to skip known-bad sources.
     */
    public boolean isCircuitBroken(String sourceType, Long sourceId) {
        Integer retries = jdbcClient.sql("""
                SELECT retry_count FROM ai_dead_letter
                 WHERE kind = 'embedding' AND source_type = :stype AND source_id = :sid
                   AND resolved_at IS NULL
                """)
                .param("stype", sourceType)
                .param("sid", sourceId)
                .query(Integer.class)
                .optional()
                .orElse(0);
        return retries >= MAX_RETRIES;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * @param processedCount number of chunks successfully embedded (0 when skipped)
     * @param tokensUsed     upstream token usage (0 when skipped)
     */
    public record IngestionOutcome(boolean processed, int processedCount, int tokensUsed, boolean sourceMissing) {
        public static IngestionOutcome processed(int count, int tokens) {
            return new IngestionOutcome(true, count, tokens, false);
        }
        public static IngestionOutcome skipped() {
            return new IngestionOutcome(false, 0, 0, false);
        }
        public static IngestionOutcome notFound() {
            return new IngestionOutcome(false, 0, 0, true);
        }
    }

    /** Used by the backfill job: list candidate source ids in batches. */
    public List<Long> listMedicalRecordIds(long afterId, int limit) {
        return jdbcClient.sql("""
                SELECT id FROM medical_record WHERE id > :after ORDER BY id ASC LIMIT :lim
                """)
                .param("after", afterId).param("lim", limit)
                .query(Long.class).list();
    }

    public List<Long> listHealthRecordIds(long afterId, int limit) {
        return jdbcClient.sql("""
                SELECT id FROM health_record WHERE id > :after ORDER BY id ASC LIMIT :lim
                """)
                .param("after", afterId).param("lim", limit)
                .query(Long.class).list();
    }

    public List<Long> listVisitRecordIds(long afterId, int limit) {
        return jdbcClient.sql("""
                SELECT id FROM visit_record WHERE id > :after ORDER BY id ASC LIMIT :lim
                """)
                .param("after", afterId).param("lim", limit)
                .query(Long.class).list();
    }

    /** Internal helper for callers (e.g. backfill dry-run reporting). */
    public Map<String, Long> sourceTableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("medical_record", count("medical_record"));
        counts.put("health_record", count("health_record"));
        counts.put("visit_record", count("visit_record"));
        return counts;
    }

    private Long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table)
                .query(Long.class).single();
    }

    /** Visible for tests. */
    static List<KnowledgeChunk> defensiveCopy(List<KnowledgeChunk> in) {
        return new ArrayList<>(in);
    }
}
