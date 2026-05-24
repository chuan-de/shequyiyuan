-- Phase 2.1: enable the pgvector extension on the hospital database.
-- Requires the running PostgreSQL container to be built from the pgvector
-- image (see docker-compose.yml — pgvector/pgvector:pg16). Integration tests
-- also pin to the same image (`pgvector/pgvector:pg16`) so this script can
-- run unmodified there.
--
-- Idempotent: CREATE EXTENSION IF NOT EXISTS is a no-op on re-run.

CREATE EXTENSION IF NOT EXISTS vector;
