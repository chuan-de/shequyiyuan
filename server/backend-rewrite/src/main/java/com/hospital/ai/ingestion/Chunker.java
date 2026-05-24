package com.hospital.ai.ingestion;

import java.util.List;

/**
 * Strategy for slicing a single domain entity into per-field
 * {@link KnowledgeChunk}s. One implementation per source type
 * (medical_record, health_record, visit).
 *
 * <p>Implementations MUST:</p>
 * <ul>
 *   <li>Skip null / blank fields — never emit an empty chunk.</li>
 *   <li>Populate {@code patientId} on every chunk so the storage layer can
 *       enforce the privacy fence.</li>
 *   <li>Include enough metadata for the retrieval citation UI to show
 *       provenance (date + actor when available).</li>
 * </ul>
 */
public interface Chunker<T> {

    List<KnowledgeChunk> chunk(T source);
}
