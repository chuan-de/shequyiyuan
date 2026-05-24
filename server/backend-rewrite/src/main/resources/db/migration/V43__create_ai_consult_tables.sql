-- Phase 3.1: community AI consult sessions + messages.
--
-- One session per chat thread (left rail in the UI). Title defaults to "新对话"
-- and may be auto-derived from the first user message (truncated to 50 chars).
-- updated_at is bumped whenever a new message lands so the session list can
-- sort recent-first without an extra join.
--
-- ai_consult_message stores the entire turn-by-turn history including the
-- system primer. Token counts and model are recorded only for assistant rows
-- (user/system rows leave them null). status = 'refused_by_guardrail' marks
-- replies generated locally without calling the LLM (e.g. off-topic refusals)
-- so analytics can separate model traffic from guardrail bounces.

CREATE TABLE IF NOT EXISTS ai_consult_session (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title       VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_consult_session_user
    ON ai_consult_session(user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_consult_message (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL REFERENCES ai_consult_session(id) ON DELETE CASCADE,
    role        VARCHAR(16)  NOT NULL,
    content     TEXT         NOT NULL,
    tokens_in   INTEGER,
    tokens_out  INTEGER,
    model       VARCHAR(64),
    status      VARCHAR(16)  NOT NULL DEFAULT 'completed',
    error_msg   TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_consult_message_session
    ON ai_consult_message(session_id, created_at);
