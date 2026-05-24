-- Phase 2.3: patient AI consent timestamp + ingestion dead-letter queue.
--
-- ai_consent_at: nullable, populated when the patient (or someone on their
-- behalf) accepts the AI processing consent. PatientRagController enforces
-- it on every /ask call — null => HTTP 412 Precondition Required => UI pops
-- the consent modal.
--
-- ai_dead_letter: every failed ingestion / embedding / extraction attempt
-- is recorded so an admin can replay or triage. retry_count tracks how many
-- times we've already tried; KnowledgeIngestionService caps retries at 5.

ALTER TABLE patient_profile
    ADD COLUMN IF NOT EXISTS ai_consent_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS ai_dead_letter (
    id              BIGSERIAL    PRIMARY KEY,
    kind            VARCHAR(32)  NOT NULL,   -- embedding / extraction / chat
    source_type     VARCHAR(32),
    source_id       BIGINT,
    patient_id      BIGINT,
    payload         JSONB        NOT NULL,
    error_msg       TEXT,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_dead_letter_unresolved
    ON ai_dead_letter(kind, created_at)
    WHERE resolved_at IS NULL;
