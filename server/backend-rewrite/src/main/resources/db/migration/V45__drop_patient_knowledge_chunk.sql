-- Phase 2 (storage migration): patient knowledge vectors now live in Qdrant
-- (see com.hospital.ai.qdrant). This drops the legacy pgvector-backed table.
-- The vector extension is intentionally kept — it's cheap and may be useful
-- later for non-AI use cases.
--
-- The table was effectively empty in production: V44 (the 2048-dim widening)
-- never completed successfully because pgvector HNSW caps at 2000 dims, so
-- every ingest attempt since the doubao-embedding-vision swap failed up
-- front. Nothing to migrate out.

DROP INDEX IF EXISTS idx_pkc_embedding_hnsw;
DROP INDEX IF EXISTS idx_pkc_patient_id;
DROP INDEX IF EXISTS idx_pkc_source;
DROP TABLE IF EXISTS patient_knowledge_chunk;
