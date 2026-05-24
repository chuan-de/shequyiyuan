-- Delete permissions for all entity-management modules.

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('doctors:delete',         '删除医生'),
  ('family-doctors:delete',  '删除家庭医生'),
  ('patients:delete',        '删除患者'),
  ('receptions:delete',      '删除前台'),
  ('medications:delete',     '删除药品'),
  ('visits:delete',          '删除就诊记录'),
  ('medical-records:delete', '删除病历'),
  ('health-records:delete',  '删除健康档案'),
  ('configs:delete',         '删除系统配置')
ON CONFLICT (permission_code) DO NOTHING;

-- Grant all delete permissions to ADMIN.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
    'doctors:delete', 'family-doctors:delete', 'patients:delete', 'receptions:delete',
    'medications:delete', 'visits:delete', 'medical-records:delete', 'health-records:delete',
    'configs:delete'
  )
ON CONFLICT DO NOTHING;
