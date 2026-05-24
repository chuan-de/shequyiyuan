-- Phase 1: medical_record AI vision OCR persistence.
-- Stores the *accepted* AI suggestions on the medical_record row itself,
-- plus a full append-only history of every extraction attempt (success /
-- partial / failed) so we can audit prompt vs. response and let medics
-- re-view past suggestions even after editing the record.

ALTER TABLE medical_record
    ADD COLUMN IF NOT EXISTS ai_extracted JSONB;

CREATE TABLE IF NOT EXISTS ai_extraction_history (
    id              BIGSERIAL    PRIMARY KEY,
    source_type     VARCHAR(32)  NOT NULL,   -- Currently only 'medical_record'; reserved for future RAG / consult flows.
    source_id       BIGINT,                  -- Nullable: extraction can happen before the medical record is persisted.
    photo_id        UUID,                    -- References photo.id; no FK so photo deletions don't cascade-kill history.
    operator_id     BIGINT,                  -- app_user.id of the medic who triggered the extraction.
    model           VARCHAR(64)  NOT NULL,
    raw_json        JSONB        NOT NULL,   -- Full upstream response payload; on failure stores {"error": "..."}.
    confidence      NUMERIC(5,2),            -- 0–100. Heuristic = (non-null fields / total fields) × 100.
    tokens_in       INTEGER,
    tokens_out      INTEGER,
    latency_ms      INTEGER,
    status          VARCHAR(16)  NOT NULL,   -- success / partial / failed
    error_msg       TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_extraction_source
    ON ai_extraction_history(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_ai_extraction_operator
    ON ai_extraction_history(operator_id, created_at DESC);
