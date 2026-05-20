-- Preconditions:
-- 1) requires V8/V9 已执行（app_permission 已存在且 permission_code 已初始化）
-- 2) this migration is the only place to evolve app_permission structure after table creation
-- 3) supports legacy V8 库（无 resource_code/action_code）与新环境线性迁移
ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS resource_code VARCHAR(50);

ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS action_code VARCHAR(50);

UPDATE app_permission
SET
    resource_code = split_part(permission_code, ':', 1),
    action_code = CASE
        WHEN position(':' IN permission_code) = 0 THEN 'read'
        ELSE split_part(permission_code, ':', array_length(string_to_array(permission_code, ':'), 1))
    END
WHERE resource_code IS NULL OR action_code IS NULL;

ALTER TABLE app_permission
    ALTER COLUMN resource_code SET NOT NULL;

ALTER TABLE app_permission
    ALTER COLUMN action_code SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_permission_resource_action
    ON app_permission(resource_code, action_code);
