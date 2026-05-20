ALTER TABLE dictionary_item
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE dictionary_item
    ADD CONSTRAINT uk_dictionary_item_dict_item UNIQUE (dict_code, item_code);

CREATE TABLE IF NOT EXISTS dictionary_operation_log (
    id BIGSERIAL PRIMARY KEY,
    dictionary_item_id BIGINT,
    operation_type VARCHAR(20) NOT NULL,
    operator VARCHAR(100) NOT NULL,
    before_value TEXT,
    after_value TEXT,
    operated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dictionary_log_item ON dictionary_operation_log(dictionary_item_id);
