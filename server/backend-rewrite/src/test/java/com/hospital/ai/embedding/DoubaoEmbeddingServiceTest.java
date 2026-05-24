package com.hospital.ai.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.hospital.ai.config.AiProperties;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the parse + dimension-check logic of
 * {@link DoubaoEmbeddingService} — the parts that don't need a live HTTP
 * call. Collaborators (RestClient, rate-limiter, audit repo) are passed as
 * null; {@code parse()} never touches them, which is exactly the
 * separation-of-concerns we want.
 */
class DoubaoEmbeddingServiceTest {

    @Test
    void parse_extracts_vectors_and_token_usage() {
        AiProperties props = new AiProperties();
        props.setEmbeddingDimension(3);
        DoubaoEmbeddingService svc = new DoubaoEmbeddingService(null, props, null, null, null);

        Map<String, Object> response = Map.of(
                "data", List.of(
                        Map.of("embedding", List.of(0.1, 0.2, 0.3)),
                        Map.of("embedding", List.of(0.4, 0.5, 0.6))),
                "usage", Map.of("prompt_tokens", 12, "completion_tokens", 0));

        EmbeddingService.EmbeddingBatchResult result = svc.parse(response, 2, 999L);

        assertEquals(2, result.vectors().size());
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result.vectors().get(0), 1e-6f);
        assertArrayEquals(new float[]{0.4f, 0.5f, 0.6f}, result.vectors().get(1), 1e-6f);
        assertEquals(12, result.tokensIn());
        assertEquals(999L, result.latencyMs());
    }

    @Test
    void parse_rejects_dimension_mismatch_with_clear_error() {
        AiProperties props = new AiProperties();
        props.setEmbeddingDimension(1024);
        DoubaoEmbeddingService svc = new DoubaoEmbeddingService(null, props, null, null, null);

        Map<String, Object> response = Map.of(
                "data", List.of(Map.of("embedding", List.of(0.1, 0.2))));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> svc.parse(response, 1, 50L));
        assertTrue(ex.getMessage().contains("dimension mismatch"));
        assertTrue(ex.getMessage().contains("1024"));
    }

    @Test
    void parse_rejects_count_mismatch() {
        AiProperties props = new AiProperties();
        DoubaoEmbeddingService svc = new DoubaoEmbeddingService(null, props, null, null, null);
        Map<String, Object> response = Map.of("data", List.of(
                Map.of("embedding", List.of(0.1, 0.2, 0.3))));
        // expected 2, got 1 → throw.
        assertThrows(IllegalStateException.class, () -> svc.parse(response, 2, 0L));
    }
}
