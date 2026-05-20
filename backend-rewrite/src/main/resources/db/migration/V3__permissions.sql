CREATE TABLE IF NOT EXISTS app_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_role_permission (
    role_id BIGINT NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES app_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO app_permission (permission_code, permission_name)
VALUES
  ('dictionary:read', '字典读取'),
  ('doctors:read', '医生读取'), ('doctors:write', '医生写入'), ('doctors:status', '医生状态变更'),
  ('medication:read', '药品读取'), ('medication:write', '药品写入'), ('medication:status', '药品状态变更'),
  ('familyDoctor:read', '家庭医生读取'), ('familyDoctor:write', '家庭医生写入'), ('familyDoctor:status', '家庭医生状态变更'),
  ('visit:read', '就诊读取'), ('visit:write', '就诊写入'), ('visit:status', '就诊状态变更'),
  ('config:read', '配置读取'), ('config:write', '配置写入'), ('config:status', '配置状态变更')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code = 'dictionary:read'
WHERE r.role_code IN ('ADMIN','USER')
ON CONFLICT DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.permission_code IN (
  'doctors:read','doctors:write','doctors:status',
  'medication:read','medication:write','medication:status',
  'familyDoctor:read','familyDoctor:write','familyDoctor:status',
  'visit:read','visit:write','visit:status',
  'config:read','config:write','config:status'
)
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;
