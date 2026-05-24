package com.hospital.ai.qdrant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.hospital.ai.ingestion.KnowledgeChunk;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Qdrant for the Phase 2 patient RAG vector storage. Only
 * two operations are needed at this layer: a batch {@link #upsert} for the
 * ingestion pipeline and a {@code patient_id}-filtered {@link #search} for
 * retrieval.
 *
 * <p><b>Privacy fence:</b> every {@link #search} call applies a mandatory
 * {@code match(patient_id, ?)} {@code must} clause — there is no overload that
 * skips it. The retrieval audit log in {@code PatientRagService} records the
 * resulting chunk ids so a security review can confirm no cross-patient
 * leakage after the fact.</p>
 *
 * <p><b>Point id strategy:</b> Qdrant accepts {@code long} or {@code UUID}
 * point ids. We derive a deterministic UUID from
 * {@code (source_type, source_id, field_key)} so re-running ingestion on the
 * same source row overwrites the existing point instead of creating duplicates
 * — the same idempotency guarantee the old
 * {@code uq_patient_knowledge_chunk_source} unique constraint provided.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class PatientKnowledgeStore {

    private static final Logger log = LoggerFactory.getLogger(PatientKnowledgeStore.class);

    /** Per-call deadline. Network upsert/search should complete well under this. */
    private static final long CALL_TIMEOUT_SECONDS = 30L;

    /** Payload keys — kept as constants so tests + callers stay in sync. */
    public static final String FIELD_PATIENT_ID = "patient_id";
    public static final String FIELD_SOURCE_TYPE = "source_type";
    public static final String FIELD_SOURCE_ID = "source_id";
    public static final String FIELD_FIELD_KEY = "field_key";
    public static final String FIELD_CHUNK_TEXT = "chunk_text";
    public static final String FIELD_METADATA_PREFIX = "metadata.";

    private final QdrantClient client;
    private final QdrantProperties props;

    public PatientKnowledgeStore(QdrantClient client, QdrantProperties props) {
        this.client = client;
        this.props = props;
    }

    /**
     * Batch-upsert chunks into Qdrant. Vector count MUST match chunk count;
     * the caller guarantees this. Re-running with the same source chunks
     * overwrites the existing points (deterministic id derivation).
     */
    public void upsert(List<KnowledgeChunk> chunks, List<float[]> vectors) {
        if (chunks == null || chunks.isEmpty()) return;
        if (vectors == null || vectors.size() != chunks.size()) {
            throw new IllegalArgumentException("upsert: chunks/vectors size mismatch ("
                    + chunks.size() + " vs " + (vectors == null ? "null" : vectors.size()) + ")");
        }
        List<PointStruct> points = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            points.add(toPoint(chunks.get(i), vectors.get(i)));
        }
        try {
            client.upsertAsync(props.getCollection(), points)
                    .get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Qdrant upsert", ie);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Qdrant upsert failed for "
                    + chunks.size() + " chunks: " + ex.getMessage(), ex);
        }
        log.debug("Qdrant upsert: {} points to collection '{}'", points.size(), props.getCollection());
    }

    /**
     * Search the top {@code topK} most similar chunks for {@code patientId}.
     * The {@code patient_id} filter is non-optional; we never search across
     * the whole collection.
     */
    public List<RetrievedChunk> search(long patientId, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) return List.of();
        // SECURITY: patient_id is a MUST clause. Search results cannot cross
        // patient boundaries even if topK is huge.
        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.match(FIELD_PATIENT_ID, patientId))
                .build();
        List<Float> vec = new ArrayList<>(queryVector.length);
        for (float f : queryVector) vec.add(f);
        SearchPoints req = SearchPoints.newBuilder()
                .setCollectionName(props.getCollection())
                .addAllVector(vec)
                .setLimit(topK)
                .setFilter(filter)
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .build();
        List<ScoredPoint> hits;
        try {
            hits = client.searchAsync(req).get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Qdrant search", ie);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Qdrant search failed for patient="
                    + patientId + ": " + ex.getMessage(), ex);
        }
        List<RetrievedChunk> result = new ArrayList<>(hits.size());
        for (ScoredPoint sp : hits) result.add(fromScoredPoint(sp));
        return result;
    }

    // --- mapping helpers ---------------------------------------------------

    PointStruct toPoint(KnowledgeChunk chunk, float[] vector) {
        Map<String, JsonWithInt.Value> payload = new LinkedHashMap<>();
        payload.put(FIELD_PATIENT_ID, ValueFactory.value(chunk.patientId()));
        payload.put(FIELD_SOURCE_TYPE, ValueFactory.value(chunk.sourceType()));
        payload.put(FIELD_SOURCE_ID, ValueFactory.value(chunk.sourceId()));
        payload.put(FIELD_FIELD_KEY, ValueFactory.value(chunk.fieldKey()));
        payload.put(FIELD_CHUNK_TEXT, ValueFactory.value(chunk.chunkText()));
        if (chunk.metadata() != null) {
            for (Map.Entry<String, Object> e : chunk.metadata().entrySet()) {
                JsonWithInt.Value v = toValue(e.getValue());
                if (v != null) payload.put(FIELD_METADATA_PREFIX + e.getKey(), v);
            }
        }
        return PointStruct.newBuilder()
                .setId(PointIdFactory.id(deterministicPointId(
                        chunk.sourceType(), chunk.sourceId(), chunk.fieldKey())))
                .setVectors(VectorsFactory.vectors(vector))
                .putAllPayload(payload)
                .build();
    }

    RetrievedChunk fromScoredPoint(ScoredPoint sp) {
        Map<String, JsonWithInt.Value> payload = sp.getPayloadMap();
        long patientId = readLong(payload, FIELD_PATIENT_ID);
        String sourceType = readString(payload, FIELD_SOURCE_TYPE);
        long sourceId = readLong(payload, FIELD_SOURCE_ID);
        String fieldKey = readString(payload, FIELD_FIELD_KEY);
        String chunkText = readString(payload, FIELD_CHUNK_TEXT);
        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, JsonWithInt.Value> e : payload.entrySet()) {
            if (!e.getKey().startsWith(FIELD_METADATA_PREFIX)) continue;
            metadata.put(e.getKey().substring(FIELD_METADATA_PREFIX.length()),
                    fromValue(e.getValue()));
        }
        return new RetrievedChunk(patientId, sourceType, sourceId, fieldKey,
                chunkText, metadata, sp.getScore());
    }

    /**
     * Stable UUID derived from (source_type, source_id, field_key). Same
     * three-tuple → same UUID → upsert overwrites the existing point. We use
     * UUID instead of {@code Math.abs(hash) % Long.MAX_VALUE} to keep the
     * 128-bit collision space (and because Qdrant's UUID id type is the
     * idiomatic choice for app-generated ids).
     */
    static UUID deterministicPointId(String sourceType, long sourceId, String fieldKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sourceType.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0x1F);
            md.update(Long.toString(sourceId).getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0x1F);
            md.update(fieldKey.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            long msb = 0L, lsb = 0L;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (digest[i] & 0xFF);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (digest[i] & 0xFF);
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static JsonWithInt.Value toValue(Object o) {
        if (o == null) return ValueFactory.nullValue();
        if (o instanceof String s) return ValueFactory.value(s);
        if (o instanceof Long l) return ValueFactory.value(l);
        if (o instanceof Integer i) return ValueFactory.value((long) i);
        if (o instanceof Boolean b) return ValueFactory.value(b);
        if (o instanceof Double d) return ValueFactory.value(d);
        if (o instanceof Float f) return ValueFactory.value(f.doubleValue());
        if (o instanceof Number n) return ValueFactory.value(n.doubleValue());
        if (o instanceof java.time.Instant inst) return ValueFactory.value(inst.toString());
        // Fallback: store the string form so audit/replay still works.
        return ValueFactory.value(o.toString());
    }

    private static Object fromValue(JsonWithInt.Value v) {
        return switch (v.getKindCase()) {
            case STRING_VALUE -> v.getStringValue();
            case INTEGER_VALUE -> v.getIntegerValue();
            case DOUBLE_VALUE -> v.getDoubleValue();
            case BOOL_VALUE -> v.getBoolValue();
            case NULL_VALUE, KIND_NOT_SET -> null;
            // LIST_VALUE / STRUCT_VALUE are not produced by toValue() today;
            // returning the protobuf form lets a caller still inspect them
            // without crashing.
            default -> v.toString();
        };
    }

    private static String readString(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value v = payload.get(key);
        if (v == null) return null;
        return v.getKindCase() == JsonWithInt.Value.KindCase.STRING_VALUE
                ? v.getStringValue() : v.toString();
    }

    private static long readLong(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value v = payload.get(key);
        if (v == null) return 0L;
        return switch (v.getKindCase()) {
            case INTEGER_VALUE -> v.getIntegerValue();
            case DOUBLE_VALUE -> (long) v.getDoubleValue();
            case STRING_VALUE -> safeParseLong(v.getStringValue());
            default -> 0L;
        };
    }

    private static long safeParseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}
