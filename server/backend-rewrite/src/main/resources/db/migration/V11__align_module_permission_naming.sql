-- Preconditions:
-- 1) requires V8/V9/V10 已执行（RBAC 基础数据已存在）
-- 2) this is data-adjustment migration and must run after permission seeding
INSERT INTO app_permission (permission_code, permission_name)
VALUES
  ('medications:read', '药品读取'),
  ('medications:write', '药品写入'),
  ('medications:status', '药品状态变更'),
  ('family-doctors:read', '家庭医生读取'),
  ('family-doctors:write', '家庭医生写入'),
  ('family-doctors:status', '家庭医生状态变更'),
  ('visits:read', '就诊读取'),
  ('visits:write', '就诊写入'),
  ('visits:status', '就诊状态变更')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT rp.role_id, p_new.id
FROM app_role_permission rp
JOIN app_permission p_old ON p_old.id = rp.permission_id
JOIN app_permission p_new ON p_new.permission_code = CASE p_old.permission_code
  WHEN 'medication:read' THEN 'medications:read'
  WHEN 'medication:write' THEN 'medications:write'
  WHEN 'medication:status' THEN 'medications:status'
  WHEN 'familyDoctor:read' THEN 'family-doctors:read'
  WHEN 'familyDoctor:write' THEN 'family-doctors:write'
  WHEN 'familyDoctor:status' THEN 'family-doctors:status'
  WHEN 'visit:read' THEN 'visits:read'
  WHEN 'visit:write' THEN 'visits:write'
  WHEN 'visit:status' THEN 'visits:status'
END
WHERE p_old.permission_code IN (
  'medication:read', 'medication:write', 'medication:status',
  'familyDoctor:read', 'familyDoctor:write', 'familyDoctor:status',
  'visit:read', 'visit:write', 'visit:status'
)
ON CONFLICT DO NOTHING;

DELETE FROM app_role_permission
WHERE permission_id IN (
  SELECT id FROM app_permission WHERE permission_code IN (
    'medication:read', 'medication:write', 'medication:status',
    'familyDoctor:read', 'familyDoctor:write', 'familyDoctor:status',
    'visit:read', 'visit:write', 'visit:status'
  )
);

DELETE FROM app_permission
WHERE permission_code IN (
  'medication:read', 'medication:write', 'medication:status',
  'familyDoctor:read', 'familyDoctor:write', 'familyDoctor:status',
  'visit:read', 'visit:write', 'visit:status'
);
