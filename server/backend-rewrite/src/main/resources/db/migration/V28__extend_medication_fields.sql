ALTER TABLE medication
    ADD COLUMN IF NOT EXISTS price        NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock        INTEGER       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS main_effect  TEXT,
    ADD COLUMN IF NOT EXISTS side_effect  TEXT,
    ADD COLUMN IF NOT EXISTS detail       TEXT;

ALTER TABLE medication ADD CONSTRAINT chk_medication_stock_nonneg CHECK (stock >= 0);

CREATE TABLE medication_inventory_log (
    id            BIGSERIAL PRIMARY KEY,
    medication_id BIGINT    NOT NULL REFERENCES medication(id) ON DELETE CASCADE,
    delta         INTEGER   NOT NULL,
    stock_after   INTEGER   NOT NULL,
    reason        VARCHAR(255),
    operator      VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('medications:inventory', '调整药品库存')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'medications:inventory'
ON CONFLICT DO NOTHING;
