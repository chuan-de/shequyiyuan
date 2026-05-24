-- Phase 2.2: per-patient knowledge chunks for the private RAG pipeline.
--
-- Each row is a slice of a single source document field (medical_record,
-- health_record, visit) plus its embedding. patient_id is the privacy fence
-- — every retrieval query MUST filter on it; the unique constraint on
-- (source_type, source_id, field_key) makes the ingestion service idempotent
-- so backfill + the AFTER_COMMIT event listener can both run safely.
--
-- Embedding dimension is 1024 — see docs/ai-features-plan.md (Phase 2 plan
-- assumes 1024 for doubao-embedding-text-240715). If the model returns a
-- different dimension at runtime, DoubaoEmbeddingService will fail fast with
-- a clear message and we add a new migration to widen the VECTOR(N).

CREATE TABLE IF NOT EXISTS patient_knowledge_chunk (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id) ON DELETE CASCADE,
    source_type     VARCHAR(32)  NOT NULL,   -- medical_record / health_record / visit
    source_id       BIGINT       NOT NULL,
    field_key       VARCHAR(64)  NOT NULL,   -- chief_complaint / diagnosis / prescription / exam / ...
    chunk_text      TEXT         NOT NULL,
    embedding       VECTOR(1024) NOT NULL,
    metadata        JSONB,                    -- at least: {source_created_at, doctor_id, department}
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_patient_knowledge_chunk_source UNIQUE (source_type, source_id, field_key)
);

CREATE INDEX IF NOT EXISTS idx_pkc_patient_id ON patient_knowledge_chunk(patient_id);
CREATE INDEX IF NOT EXISTS idx_pkc_source ON patient_knowledge_chunk(source_type, source_id);

-- HNSW with cosine distance. Embeddings are normalised by Doubao so cosine
-- behaves like dot-product; HNSW handles approximate nearest neighbour at
-- query time without an exact scan.
CREATE INDEX IF NOT EXISTS idx_pkc_embedding_hnsw
    ON patient_knowledge_chunk USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
