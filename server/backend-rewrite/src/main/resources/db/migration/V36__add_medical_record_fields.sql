-- Add prescription items, attachments, and record date to medical_record.
-- prescription_items stores an array of {medicationId, name, quantity}.
-- attachments stores an array of {url, filename, contentType}.
ALTER TABLE medical_record
    ADD COLUMN IF NOT EXISTS prescription_items JSONB,
    ADD COLUMN IF NOT EXISTS attachments        JSONB,
    ADD COLUMN IF NOT EXISTS record_date        TIMESTAMPTZ;
