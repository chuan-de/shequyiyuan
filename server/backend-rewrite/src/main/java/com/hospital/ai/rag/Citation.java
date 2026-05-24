package com.hospital.ai.rag;

import java.time.Instant;

/**
 * One retrieved chunk surfaced as a citation under an {@link RagAnswer}.
 * {@code snippet} is the chunk text (truncated to ~200 chars in the
 * controller layer if needed); the UI uses {@code sourceType + sourceId} to
 * deep-link back into the medical record / health record / visit detail
 * page.
 */
public record Citation(
        Long chunkId,
        String sourceType,
        Long sourceId,
        String fieldKey,
        String snippet,
        Instant sourceDate,
        Double similarity) {
}
