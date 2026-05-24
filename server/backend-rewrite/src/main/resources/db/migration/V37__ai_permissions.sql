-- Phase 0: AI feature permission codes + role grants.
-- Style mirrors V26 / V22 — ON CONFLICT DO NOTHING so a re-run is idempotent.

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('ai:vision',      'AI 病历识别'),
  ('ai:patient-rag', '患者 AI 问询'),
  ('ai:consult',     '社区 AI 问诊'),
  ('ai:admin',       'AI 审计与配额管理')
ON CONFLICT (permission_code) DO NOTHING;

-- ADMIN gets everything.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('ai:vision', 'ai:patient-rag', 'ai:consult', 'ai:admin')
ON CONFLICT DO NOTHING;

-- DOCTOR gets vision + patient-rag + consult.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.role_code = 'DOCTOR'
  AND p.permission_code IN ('ai:vision', 'ai:patient-rag', 'ai:consult')
ON CONFLICT DO NOTHING;

-- PATIENT gets patient-rag (their own data) + consult.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.role_code = 'PATIENT'
  AND p.permission_code IN ('ai:patient-rag', 'ai:consult')
ON CONFLICT DO NOTHING;
