CREATE TABLE IF NOT EXISTS system_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(200) NOT NULL UNIQUE,
    config_value TEXT         NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_system_config_status ON system_config (status);
