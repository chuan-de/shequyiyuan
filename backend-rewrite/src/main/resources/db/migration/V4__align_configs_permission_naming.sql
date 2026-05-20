-- Align config-module permission naming with frontend convention: configs:*
INSERT INTO app_permission (permission_code, permission_name)
VALUES
  ('configs:read', '配置读取'),
  ('configs:write', '配置写入'),
  ('configs:status', '配置状态变更')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT rp.role_id, p_new.id
FROM app_role_permission rp
JOIN app_permission p_old ON p_old.id = rp.permission_id
JOIN app_permission p_new ON p_new.permission_code = REPLACE(p_old.permission_code, 'config:', 'configs:')
WHERE p_old.permission_code IN ('config:read', 'config:write', 'config:status')
ON CONFLICT DO NOTHING;

DELETE FROM app_role_permission
WHERE permission_id IN (
  SELECT id FROM app_permission WHERE permission_code IN ('config:read', 'config:write', 'config:status')
);

DELETE FROM app_permission
WHERE permission_code IN ('config:read', 'config:write', 'config:status');
