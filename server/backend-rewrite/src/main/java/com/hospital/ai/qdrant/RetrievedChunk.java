package com.hospital.ai.qdrant;

import java.util.Map;

/**
 * One scored chunk returned by {@link PatientKnowledgeStore#search}. Mirrors
 * the row shape the old pgvector SQL produced so {@code PatientRagService}
 * could swap storage without touching its citation-mapping code.
 *
 * <p>{@code score} is the raw Qdrant similarity (higher = closer for Cosine).
 * Callers can use it as-is or convert to a 0..1 range.</p>
 */
public record RetrievedChunk(
        long patientId,
        String sourceType,
        long sourceId,
        String fieldKey,
        String chunkText,
        Map<String, Object> metadata,
        float score) {
}
