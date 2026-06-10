package com.hospital.ai.ingestion;

import java.util.Map;

/**
 * One slice of patient knowledge ready for embedding + storage in the
 * Qdrant {@code patient_knowledge} collection. Produced by a {@link Chunker}
 * per source row. {@code metadata} ends up flattened onto the Qdrant point
 * payload (key-prefixed with {@code metadata.}) so retrieval can show
 * provenance ({@code source_created_at}, {@code doctor_id},
 * {@code department}, etc.).
 */
public record KnowledgeChunk(
        String sourceType,
        Long sourceId,
        Long patientId,
        String fieldKey,
        String chunkText,
        Map<String, Object> metadata) {

    /** Source type for medical_record-derived chunks. */
    public static final String SOURCE_MEDICAL_RECORD = "medical_record";
    /** Source type for visit_record-derived chunks. */
    public static final String SOURCE_VISIT = "visit";
}
