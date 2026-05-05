CREATE TABLE IF NOT EXISTS app_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    resource_code VARCHAR(50) NOT NULL,
    action_code VARCHAR(50) NOT NULL,
    permission_name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_role_permission (
    role_id BIGINT NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES app_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS app_legacy_permission_mapping (
    id BIGSERIAL PRIMARY KEY,
    legacy_module VARCHAR(100) NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    api_pattern VARCHAR(200) NOT NULL,
    permission_code VARCHAR(100) NOT NULL REFERENCES app_permission(permission_code),
    role_code VARCHAR(50) NOT NULL REFERENCES app_role(role_code)
);

INSERT INTO app_permission (permission_code, resource_code, action_code, permission_name)
VALUES
    ('dictionary:read', 'dictionary', 'read', '查看字典'),
    ('dictionary:write', 'dictionary', 'write', '编辑字典'),
    ('dictionary:delete', 'dictionary', 'delete', '删除字典'),
    ('auth:me:read', 'auth', 'read', '查看当前用户信息')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'auth:me:read')
WHERE r.role_code = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'dictionary:write', 'dictionary:delete', 'auth:me:read')
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;
