-- Photo storage: keep image bytes directly in Postgres.
-- Used by Doctor / FamilyDoctor / Patient / Reception / Medication photoUrl fields.

CREATE TABLE photo (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type      VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255),
    size_bytes        BIGINT       NOT NULL,
    data              BYTEA        NOT NULL,
    uploader_id       BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photo_uploader ON photo(uploader_id);
CREATE INDEX idx_photo_created_at ON photo(created_at);
