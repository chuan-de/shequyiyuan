INSERT INTO app_permission (permission_code, permission_name)
VALUES
  ('medical-records:read', '病历读取'),
  ('medical-records:write', '病历写入'),
  ('medical-records:status', '病历状态变更'),
  ('health-records:read', '健康档案读取'),
  ('health-records:write', '健康档案写入'),
  ('health-records:status', '健康档案状态变更')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN (
  'medical-records:read','medical-records:write','medical-records:status',
  'health-records:read','health-records:write','health-records:status'
)
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;
