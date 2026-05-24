-- Phase 2 (post-launch fix): widen embedding to 2048 dims.
--
-- doubao-embedding-text-240715 (1024 dims) is not whitelisted on Volcano's
-- agentPlan tier; the only embedding model available there is
-- doubao-embedding-vision-250615, which returns 2048-float vectors. V41
-- baked 1024 into the column, so every ingest failed with
-- "Embedding dimension mismatch".
--
-- Table is empty in practice (ingestion has been crashing since the model
-- swap), so this is a cheap DROP-and-recreate of the column + index. If you
-- ever run this against a populated table you'll lose embeddings (chunks
-- can be re-embedded via the admin backfill endpoint).
--
-- HNSW indexes must be rebuilt because the vector type changes. Cosine
-- distance opclass is unchanged.

DROP INDEX IF EXISTS idx_pkc_embedding_hnsw;

ALTER TABLE patient_knowledge_chunk
    ALTER COLUMN embedding TYPE VECTOR(2048);

CREATE INDEX IF NOT EXISTS idx_pkc_embedding_hnsw
    ON patient_knowledge_chunk USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
