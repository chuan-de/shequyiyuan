package com.hospital.ai.embedding;

import java.util.List;

/**
 * Provider-agnostic seam for text-embedding calls. Phase 2 ships
 * {@link DoubaoEmbeddingService} which talks to Volcengine Ark's
 * OpenAI-compatible {@code /v1/embeddings} endpoint; future providers
 * (self-hosted models, alternative cloud) implement this interface without
 * touching the chunkers, ingestion service, or RAG layer.
 *
 * <p>Implementations MUST return vectors whose dimension equals
 * {@code AiProperties#embeddingDimension} so the {@code VECTOR(N)} column in
 * Flyway V41 keeps working. {@link DoubaoEmbeddingService} fails fast when
 * the upstream returns a different size.</p>
 */
public interface EmbeddingService {

    /**
     * Embed a batch of texts in a single upstream call. Order of {@code texts}
     * is preserved in the returned list.
     *
     * @throws com.hospital.ai.common.AiRateLimitException when the caller is
     *         over their AI quota — mapped to HTTP 429 upstream.
     */
    EmbeddingBatchResult embed(List<String> texts);

    /** Convenience for the single-text case (RAG question embedding). */
    default float[] embedOne(String text) {
        EmbeddingBatchResult result = embed(List.of(text));
        if (result.vectors().isEmpty()) {
            throw new IllegalStateException("Embedding service returned no vectors");
        }
        return result.vectors().get(0);
    }

    /** Carrier holding the vectors and the upstream token usage for audit. */
    record EmbeddingBatchResult(List<float[]> vectors, int tokensIn, int tokensOut, long latencyMs) {
        public int totalTokens() { return tokensIn + tokensOut; }
    }
}
