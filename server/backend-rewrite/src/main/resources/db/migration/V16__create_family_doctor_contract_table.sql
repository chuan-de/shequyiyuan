CREATE TABLE IF NOT EXISTS family_doctor_contract (
    id          BIGSERIAL PRIMARY KEY,
    resident_id BIGINT      NOT NULL,
    doctor_id   BIGINT      NOT NULL,
    status      VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fdc_resident ON family_doctor_contract (resident_id);
CREATE INDEX IF NOT EXISTS idx_fdc_doctor   ON family_doctor_contract (doctor_id);
CREATE INDEX IF NOT EXISTS idx_fdc_status   ON family_doctor_contract (status);
