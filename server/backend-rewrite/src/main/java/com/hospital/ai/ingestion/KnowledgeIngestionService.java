package com.hospital.ai.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.ai.embedding.EmbeddingService;
import com.hospital.ai.qdrant.PatientKnowledgeStore;
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
 * load entity → chunk → embed → batch-upsert into Qdrant (collection
 * {@code patient_knowledge}).
 *
 * <p>Failures are captured in {@code ai_dead_letter} (per source row, with a
 * retry counter) so a transient embedding outage doesn't lose work. Re-runs
 * are safe thanks to the deterministic Qdrant point id derived from
 * {@code (source_type, source_id, field_key)} — upsert overwrites the
 * existing point when the source field changes.</p>
 *
 * <p>Each public {@code ingest*} method runs in its OWN transaction
 * ({@code REQUIRES_NEW}). Qdrant writes themselves are NOT in the DB
 * transaction — they happen after the embed call and are not rolled back if
 * the surrounding transaction fails. Idempotency makes a retry safe.</p>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);
    private static final int MAX_RETRIES = 5;

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordChunker medicalRecordChunker;
    private final VisitRecordChunker visitRecordChunker;
    private final EmbeddingService embeddingService;
    private final PatientKnowledgeStore knowledgeStore;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public KnowledgeIngestionService(MedicalRecordRepository medicalRecordRepository,
                                     VisitRepository visitRepository,
                                     MedicalRecordChunker medicalRecordChunker,
                                     VisitRecordChunker visitRecordChunker,
                                     EmbeddingService embeddingService,
                                     PatientKnowledgeStore knowledgeStore,
                                     JdbcClient jdbcClient,
                                     ObjectMapper objectMapper) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.visitRepository = visitRepository;
        this.medicalRecordChunker = medicalRecordChunker;
        this.visitRecordChunker = visitRecordChunker;
        this.embeddingService = embeddingService;
        this.knowledgeStore = knowledgeStore;
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

        try {
            knowledgeStore.upsert(chunks, vectors);
        } catch (RuntimeException ex) {
            log.warn("Qdrant upsert failed for {}#{}: {}", sourceType, sourceId, ex.toString());
            recordDeadLetter("upsert", sourceType, sourceId, patientId,
                    Map.of("chunk_count", chunks.size()),
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            throw ex;
        }
        return IngestionOutcome.processed(chunks.size(), embedded.totalTokens());
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
