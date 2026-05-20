-- Preconditions:
-- 1) requires V9 已执行（app_legacy_permission_mapping 已存在）
-- 2) requires referenced app_permission.permission_code and app_role.role_code already exist
-- 3) this is data-only migration and must run after RBAC baseline migrations
INSERT INTO app_legacy_permission_mapping (legacy_module, menu_name, api_pattern, permission_code, role_code)
VALUES
    ('dictionary', '字典管理', '/dictionary/**', 'dictionary:read', 'USER'),
    ('dictionary', '字典管理', '/dictionary/**', 'dictionary:write', 'ADMIN'),
    ('dictionary', '字典管理', '/dictionary/**', 'dictionary:delete', 'ADMIN'),
    ('users', '用户管理', '/users/**', 'dictionary:read', 'ADMIN'),
    ('yisheng', '医生管理', '/yisheng/**', 'dictionary:read', 'ADMIN'),
    ('yaopin', '药品管理', '/yaopin/**', 'dictionary:read', 'ADMIN'),
    ('jiuzhen', '就诊管理', '/jiuzhen/**', 'dictionary:read', 'ADMIN')
ON CONFLICT DO NOTHING;
