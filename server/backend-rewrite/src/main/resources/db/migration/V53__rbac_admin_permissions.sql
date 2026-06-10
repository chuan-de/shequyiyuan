-- 角色权限配置页：查看/编辑各角色的权限分配。
INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('rbac:read',  '查看角色权限'),
  ('rbac:write', '配置角色权限')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code IN ('rbac:read', 'rbac:write')
ON CONFLICT DO NOTHING;
