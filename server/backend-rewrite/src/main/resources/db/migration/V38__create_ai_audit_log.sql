-- Phase 0: persistent audit trail of every AI provider call.
-- Schema matches docs/ai-features-plan.md section 5.

CREATE TABLE IF NOT EXISTS ai_audit_log (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT,
    feature          VARCHAR(32)  NOT NULL,
    model            VARCHAR(64),
    prompt_excerpt   TEXT,
    response_excerpt TEXT,
    tokens_in        INTEGER,
    tokens_out       INTEGER,
    latency_ms       INTEGER,
    status           VARCHAR(16)  NOT NULL,
    error_msg        TEXT,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_audit_log_user_created
    ON ai_audit_log (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_audit_log_feature_created
    ON ai_audit_log (feature, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_audit_log_trace_id
    ON ai_audit_log (trace_id);
