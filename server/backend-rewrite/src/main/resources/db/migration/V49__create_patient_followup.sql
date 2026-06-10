-- 慢病随访记录：结构化健康指标（血压/血糖/身高体重/心率），支撑趋势分析。
CREATE TABLE patient_followup (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id) ON DELETE CASCADE,
    measured_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    systolic        INTEGER,                 -- 收缩压 mmHg
    diastolic       INTEGER,                 -- 舒张压 mmHg
    blood_sugar     NUMERIC(5,2),            -- 血糖 mmol/L
    height_cm       NUMERIC(5,1),            -- 身高 cm
    weight_kg       NUMERIC(5,1),            -- 体重 kg
    heart_rate      INTEGER,                 -- 心率 bpm
    notes           TEXT,                    -- 随访备注
    recorded_by     VARCHAR(100),            -- 录入人（操作账号）
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_followup_patient_time ON patient_followup(patient_id, measured_at DESC);

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('followups:read',   '查看随访记录'),
  ('followups:write',  '录入/编辑随访记录'),
  ('followups:delete', '删除随访记录')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('followups:read', 'followups:write', 'followups:delete')
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code IN ('DOCTOR', 'FAMILY_DOCTOR') AND p.permission_code IN ('followups:read', 'followups:write')
ON CONFLICT DO NOTHING;
