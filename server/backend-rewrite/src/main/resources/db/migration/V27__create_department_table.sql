CREATE TABLE department (
    id          BIGSERIAL    PRIMARY KEY,
    dept_name   VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    head_person VARCHAR(100),
    phone       VARCHAR(20),
    dept_types  INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO app_permission (permission_code, permission_name) VALUES
  ('departments:read',  '查看科室'),
  ('departments:write', '创建/编辑科室'),
  ('departments:delete','删除科室')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('departments:read','departments:write','departments:delete')
ON CONFLICT DO NOTHING;
