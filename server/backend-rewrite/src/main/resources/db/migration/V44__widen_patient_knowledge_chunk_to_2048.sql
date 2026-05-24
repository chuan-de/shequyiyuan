-- Phase 2 (post-launch fix) — superseded by V45.
--
-- Originally tried to widen patient_knowledge_chunk.embedding to vector(2048)
-- so doubao-embedding-vision-250615 (2048-dim) would fit. That failed
-- because pgvector's HNSW index caps the vector type at 2000 dimensions,
-- which left this migration in success=false on any DB that picked it up.
--
-- Rather than work around the HNSW limit, the whole table was dropped by
-- V45 in favour of Qdrant (see com.hospital.ai.qdrant). This file is kept
-- as a no-op marker so the version history is monotonic and existing
-- environments that recorded V44 as failed can be repaired (flyway repair)
-- and re-run cleanly.
--
-- If you are seeing V44 with success=false in flyway_schema_history, run:
--     DELETE FROM flyway_schema_history WHERE version='44' AND success=false;
-- then restart the app; V44 will re-run as this no-op and V45 will follow.

SELECT 1;
