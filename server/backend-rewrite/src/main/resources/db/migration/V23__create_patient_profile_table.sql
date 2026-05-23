CREATE TABLE IF NOT EXISTS patient_profile (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    full_name  VARCHAR(100) NOT NULL,
    photo_url  VARCHAR(255),
    sex_types  INTEGER,
    phone      VARCHAR(20)  UNIQUE,
    id_number  VARCHAR(30)  UNIQUE,
    email      VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_patient_profile_user_id ON patient_profile (user_id);
