-- 家庭医生签约：患者 ↔ 家庭医生的服务契约（社区医院核心业务）。
CREATE TABLE family_doctor_contract (
    id               BIGSERIAL    PRIMARY KEY,
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id) ON DELETE CASCADE,
    family_doctor_id BIGINT       NOT NULL REFERENCES family_doctor_profile(id) ON DELETE CASCADE,
    service_package  VARCHAR(100),                       -- 服务包（基础包/慢病管理包/老年照护包…）
    signed_at        DATE         NOT NULL,
    expires_at       DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / TERMINATED / EXPIRED
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 一个患者同一时间只能有一份生效中的签约。
CREATE UNIQUE INDEX uq_fdc_active_per_patient ON family_doctor_contract(patient_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_fdc_doctor ON family_doctor_contract(family_doctor_id);

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('family-doctor-contracts:read',  '查看家庭医生签约'),
  ('family-doctor-contracts:write', '办理/解除家庭医生签约')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN (
    'family-doctor-contracts:read', 'family-doctor-contracts:write'
) ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'FAMILY_DOCTOR' AND p.permission_code IN (
    'family-doctor-contracts:read', 'family-doctor-contracts:write'
) ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'DOCTOR' AND p.permission_code = 'family-doctor-contracts:read'
ON CONFLICT DO NOTHING;
