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
    ('medications:read', 'medications', 'read', '查看药品'),
    ('medications:write', 'medications', 'write', '编辑药品'),
    ('medications:status', 'medications', 'status', '变更药品状态'),
    ('family-doctors:read', 'family-doctors', 'read', '查看家庭医生签约'),
    ('family-doctors:write', 'family-doctors', 'write', '编辑家庭医生签约'),
    ('family-doctors:status', 'family-doctors', 'status', '变更家庭医生签约状态'),
    ('visits:read', 'visits', 'read', '查看就诊记录'),
    ('visits:write', 'visits', 'write', '编辑就诊记录'),
    ('visits:status', 'visits', 'status', '变更就诊记录状态'),
    ('configs:read', 'configs', 'read', '查看系统配置'),
    ('configs:write', 'configs', 'write', '编辑系统配置'),
    ('configs:status', 'configs', 'status', '变更系统配置状态'),
    ('auth:me:read', 'auth', 'read', '查看当前用户信息')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'medications:read', 'family-doctors:read', 'visits:read', 'configs:read', 'auth:me:read')
WHERE r.role_code = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'dictionary:write', 'dictionary:delete', 'medications:read', 'medications:write', 'medications:status', 'family-doctors:read', 'family-doctors:write', 'family-doctors:status', 'visits:read', 'visits:write', 'visits:status', 'configs:read', 'configs:write', 'configs:status', 'auth:me:read')
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;
