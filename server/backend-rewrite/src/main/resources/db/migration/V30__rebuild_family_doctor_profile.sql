DROP TABLE IF EXISTS family_doctor_contract;

CREATE TABLE family_doctor_profile (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    full_name   VARCHAR(100) NOT NULL,
    photo_url   VARCHAR(255),
    sex_types   INTEGER,
    phone       VARCHAR(20)  UNIQUE,
    email       VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_family_doctor_user_id   ON family_doctor_profile(user_id);
CREATE INDEX idx_family_doctor_full_name ON family_doctor_profile(full_name);
