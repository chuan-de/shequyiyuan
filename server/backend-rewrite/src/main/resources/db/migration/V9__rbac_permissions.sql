-- Preconditions:
-- 1) requires V8 already执行完成（app_permission/app_role_permission 结构已存在）
-- 2) requires app_role already exists
-- 3) this script仅负责数据初始化与 app_legacy_permission_mapping 建表，不做 app_permission 结构变更
CREATE TABLE IF NOT EXISTS app_legacy_permission_mapping (
    id BIGSERIAL PRIMARY KEY,
    legacy_module VARCHAR(100) NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    api_pattern VARCHAR(200) NOT NULL,
    permission_code VARCHAR(100) NOT NULL REFERENCES app_permission(permission_code),
    role_code VARCHAR(50) NOT NULL REFERENCES app_role(role_code)
);

INSERT INTO app_permission (permission_code, permission_name)
VALUES
    ('dictionary:read', '查看字典'),
    ('dictionary:write', '编辑字典'),
    ('dictionary:delete', '删除字典'),
    ('medications:read', '查看药品'),
    ('medications:write', '编辑药品'),
    ('medications:status', '变更药品状态'),
    ('family-doctors:read', '查看家庭医生签约'),
    ('family-doctors:write', '编辑家庭医生签约'),
    ('family-doctors:status', '变更家庭医生签约状态'),
    ('visits:read', '查看就诊记录'),
    ('visits:write', '编辑就诊记录'),
    ('visits:status', '变更就诊记录状态'),
    ('configs:read', '查看系统配置'),
    ('configs:write', '编辑系统配置'),
    ('configs:status', '变更系统配置状态'),
    ('auth:me:read', '查看当前用户信息')
ON CONFLICT (permission_code) DO NOTHING;

-- Preconditions for data seed:
-- requires app_permission/app_role_permission already exists in this script,
-- and app_role contains USER role code.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'medications:read', 'family-doctors:read', 'visits:read', 'configs:read', 'auth:me:read')
WHERE r.role_code = 'USER'
ON CONFLICT DO NOTHING;

-- Preconditions for data seed:
-- requires app_permission/app_role_permission already exists in this script,
-- and app_role contains ADMIN role code.
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN ('dictionary:read', 'dictionary:write', 'dictionary:delete', 'medications:read', 'medications:write', 'medications:status', 'family-doctors:read', 'family-doctors:write', 'family-doctors:status', 'visits:read', 'visits:write', 'visits:status', 'configs:read', 'configs:write', 'configs:status', 'auth:me:read')
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;
