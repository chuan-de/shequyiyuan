INSERT INTO app_role (role_code, role_name) VALUES ('DOCTOR', '医生')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('doctors:reset-password',    '重置医生密码'),
  ('doctors:status',            '启用/禁用医生'),
  ('patients:reset-password',   '重置患者密码'),
  ('receptions:reset-password', '重置前台密码')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('doctors:reset-password','doctors:status','patients:reset-password','receptions:reset-password')
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'DOCTOR' AND p.permission_code IN ('visits:read','medical-records:read')
ON CONFLICT DO NOTHING;
