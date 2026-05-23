CREATE TABLE IF NOT EXISTS reception_profile (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    uuid_number VARCHAR(50)  UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    photo_url   VARCHAR(255),
    sex_types   INTEGER,
    phone       VARCHAR(20)  UNIQUE,
    email       VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reception_profile_user_id ON reception_profile (user_id);
