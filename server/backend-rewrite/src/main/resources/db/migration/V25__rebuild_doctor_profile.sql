DROP TABLE IF EXISTS doctor_profile;

CREATE TABLE doctor_profile (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    uuid_number VARCHAR(50)  UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    photo_url   VARCHAR(255),
    sex_types   INTEGER,
    phone       VARCHAR(20)  UNIQUE,
    id_number   VARCHAR(30)  UNIQUE,
    email       VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doctor_profile_user_id     ON doctor_profile(user_id);
CREATE INDEX idx_doctor_profile_uuid_number ON doctor_profile(uuid_number);
CREATE INDEX idx_doctor_profile_full_name   ON doctor_profile(full_name);
