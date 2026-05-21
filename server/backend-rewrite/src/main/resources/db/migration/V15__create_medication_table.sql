CREATE TABLE IF NOT EXISTS medication (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(100) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_medication_status ON medication (status);
CREATE INDEX IF NOT EXISTS idx_medication_code   ON medication (code);
