INSERT INTO app_role (role_code, role_name) VALUES ('FAMILY_DOCTOR', '家庭医生')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('family-doctors:read',           '查看家庭医生'),
  ('family-doctors:write',          '创建/编辑家庭医生'),
  ('family-doctors:status',         '启用/禁用家庭医生'),
  ('family-doctors:reset-password', '重置家庭医生密码')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN (
    'family-doctors:read', 'family-doctors:write', 'family-doctors:status', 'family-doctors:reset-password'
) ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'FAMILY_DOCTOR' AND p.permission_code IN ('family-doctors:read', 'health-records:read', 'visits:read')
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'PATIENT' AND p.permission_code = 'family-doctors:read'
ON CONFLICT DO NOTHING;
