-- Preconditions: V8/V9 (app_permission/app_role_permission already exist)
INSERT INTO app_role (role_code, role_name)
VALUES
    ('PATIENT',   '患者'),
    ('RECEPTION', '前台')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO app_permission (permission_code, permission_name)
VALUES
    ('patients:read',    '查看患者'),
    ('patients:write',   '创建/编辑患者'),
    ('patients:status',  '变更患者状态'),
    ('receptions:read',  '查看前台'),
    ('receptions:write', '创建/编辑前台'),
    ('receptions:status','变更前台状态')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
      'patients:read', 'patients:write', 'patients:status',
      'receptions:read', 'receptions:write', 'receptions:status'
  )
ON CONFLICT DO NOTHING;
