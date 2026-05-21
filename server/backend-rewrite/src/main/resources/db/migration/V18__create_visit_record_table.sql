CREATE TABLE IF NOT EXISTS visit_record (
    id           BIGSERIAL PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL,
    doctor_name  VARCHAR(100) NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visit_record_status ON visit_record (status);
