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
