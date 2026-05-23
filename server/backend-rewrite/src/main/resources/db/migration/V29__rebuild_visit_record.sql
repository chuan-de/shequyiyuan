DROP TABLE IF EXISTS visit_record;

CREATE TABLE visit_record (
    id                  BIGSERIAL    PRIMARY KEY,
    patient_id          BIGINT       NOT NULL REFERENCES patient_profile(id) ON DELETE RESTRICT,
    visit_number        VARCHAR(50)  NOT NULL UNIQUE,
    fee                 NUMERIC(10,2),
    keshi_types         INTEGER,
    visit_date          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    registration_notes  TEXT,
    visit_content       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visit_patient     ON visit_record(patient_id);
CREATE INDEX idx_visit_number      ON visit_record(visit_number);
CREATE INDEX idx_visit_keshi       ON visit_record(keshi_types);
