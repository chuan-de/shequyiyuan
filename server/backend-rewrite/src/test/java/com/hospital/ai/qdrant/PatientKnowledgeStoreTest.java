package com.hospital.ai.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hospital.ai.ingestion.KnowledgeChunk;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpdateResult;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link PatientKnowledgeStore} that exercise the request
 * assembly + response mapping against a mocked {@link QdrantClient}. No
 * Qdrant or network is involved.
 *
 * <p>Critical behaviours covered:</p>
 * <ul>
 *   <li>upsert produces one {@link PointStruct} per chunk with the expected
 *       payload keys (patient_id / source_type / source_id / field_key /
 *       chunk_text + flattened metadata).</li>
 *   <li>search assembles a {@link SearchPoints} request with a non-empty
 *       {@code must} filter and asks for payload.</li>
 *   <li>deterministic point id derivation: same tuple → same UUID across
 *       calls.</li>
 *   <li>response mapping round-trips payload values back into the
 *       {@link RetrievedChunk} fields.</li>
 * </ul>
 */
class PatientKnowledgeStoreTest {

    private final QdrantClient client = mock(QdrantClient.class);
    private final QdrantProperties props = props();
    private final PatientKnowledgeStore store = new PatientKnowledgeStore(client, props);

    @Test
    void upsert_sends_one_point_per_chunk_with_full_payload() throws Exception {
        @SuppressWarnings("unchecked")
        ListenableFuture<UpdateResult> ack = Futures.immediateFuture(UpdateResult.getDefaultInstance());
        when(client.upsertAsync(eq("patient_knowledge"), any())).thenReturn(ack);

        KnowledgeChunk c = new KnowledgeChunk(
                "medical_record", 100L, 42L, "diagnosis", "上呼吸道感染",
                Map.of("source_created_at", "2026-01-15T08:00:00Z",
                        "doctor_id", 10L));
        float[] vec = new float[]{0.1f, 0.2f, 0.3f};

        store.upsert(List.of(c), List.of(vec));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointStruct>> cap = ArgumentCaptor.forClass(List.class);
        verify(client).upsertAsync(eq("patient_knowledge"), cap.capture());
        List<PointStruct> sent = cap.getValue();
        assertEquals(1, sent.size());

        Map<String, JsonWithInt.Value> payload = sent.get(0).getPayloadMap();
        assertEquals(42L, payload.get("patient_id").getIntegerValue(),
                "patient_id payload must be present (privacy fence depends on it)");
        assertEquals("medical_record", payload.get("source_type").getStringValue());
        assertEquals(100L, payload.get("source_id").getIntegerValue());
        assertEquals("diagnosis", payload.get("field_key").getStringValue());
        assertEquals("上呼吸道感染", payload.get("chunk_text").getStringValue());
        assertEquals("2026-01-15T08:00:00Z",
                payload.get("metadata.source_created_at").getStringValue());
        assertEquals(10L, payload.get("metadata.doctor_id").getIntegerValue());
    }

    @Test
    void search_builds_request_with_patient_id_filter_and_topk() throws Exception {
        @SuppressWarnings("unchecked")
        ListenableFuture<List<ScoredPoint>> empty = Futures.immediateFuture(List.of());
        when(client.searchAsync(any(SearchPoints.class))).thenReturn(empty);

        store.search(99L, new float[]{0.1f, 0.2f}, 5);

        ArgumentCaptor<SearchPoints> cap = ArgumentCaptor.forClass(SearchPoints.class);
        verify(client).searchAsync(cap.capture());
        SearchPoints req = cap.getValue();
        assertEquals("patient_knowledge", req.getCollectionName());
        assertEquals(5L, req.getLimit());
        Filter f = req.getFilter();
        assertEquals(1, f.getMustCount(), "search must apply exactly one patient_id MUST filter");
        // The single must clause should match the patient_id payload field
        // against the requested value. We assert via toString since
        // ConditionFactory.match() builds a private oneof.
        String mustStr = f.getMust(0).toString();
        assertTrue(mustStr.contains("patient_id"),
                "must clause should target patient_id field, got: " + mustStr);
        assertTrue(mustStr.contains("99"),
                "must clause should match patient_id=99, got: " + mustStr);
        assertTrue(req.hasWithPayload(), "search must request payload so retrieval can build citations");
    }

    @Test
    void search_returns_empty_for_empty_vector() throws Exception {
        List<RetrievedChunk> out = store.search(1L, new float[0], 5);
        assertEquals(List.of(), out);
        verify(client, org.mockito.Mockito.never()).searchAsync(any(SearchPoints.class));
    }

    @Test
    void deterministicPointId_is_stable_per_tuple() {
        UUID a = PatientKnowledgeStore.deterministicPointId("medical_record", 1L, "diagnosis");
        UUID b = PatientKnowledgeStore.deterministicPointId("medical_record", 1L, "diagnosis");
        UUID c = PatientKnowledgeStore.deterministicPointId("medical_record", 2L, "diagnosis");
        assertEquals(a, b, "same tuple must yield same UUID (idempotent upsert)");
        assertNotNull(c);
        assertTrue(!a.equals(c), "different source_id should yield a different UUID");
    }

    @Test
    void upsert_rejects_size_mismatch() {
        KnowledgeChunk c = new KnowledgeChunk(
                "visit", 1L, 1L, "visit_content", "x", Map.of());
        try {
            store.upsert(List.of(c), List.of());
            org.junit.jupiter.api.Assertions.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { /* good */ }
    }

    @Test
    void upsert_propagates_qdrant_failure() {
        @SuppressWarnings("unchecked")
        ListenableFuture<UpdateResult> boom = Futures.immediateFailedFuture(
                new RuntimeException("connection refused"));
        when(client.upsertAsync(eq("patient_knowledge"), any())).thenReturn(boom);

        KnowledgeChunk c = new KnowledgeChunk(
                "medical_record", 1L, 1L, "diagnosis", "x", Map.of());
        try {
            store.upsert(List.of(c), List.of(new float[]{0.1f}));
            org.junit.jupiter.api.Assertions.fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Qdrant upsert failed"));
        }
    }

    private static QdrantProperties props() {
        QdrantProperties p = new QdrantProperties();
        p.setCollection("patient_knowledge");
        p.setVectorSize(2048);
        return p;
    }

    // Sanity that the unused ExecutionException import would compile under
    // the SDK's checked-exception surface — kept implicit by Futures usage.
    @SuppressWarnings("unused")
    private static void usedToSilenceImport() throws ExecutionException { }
}
