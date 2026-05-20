-- Consolidated migration for app_permission.resource_code/action_code
-- Covers both schema evolution and historical data backfill in one Flyway version.
ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS resource_code VARCHAR(50);

ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS action_code VARCHAR(50);

-- Backfill historical rows from permission_code.
UPDATE app_permission
SET resource_code = LEFT(COALESCE(NULLIF(split_part(permission_code, ':', 1), ''), 'unknown'), 50)
WHERE resource_code IS NULL;

UPDATE app_permission
SET action_code = LEFT(
        COALESCE(
            NULLIF(split_part(permission_code, ':', array_length(string_to_array(permission_code, ':'), 1)), ''),
            'unknown'
        ),
        50
    )
WHERE action_code IS NULL;

-- Defensive defaults + null cleanup for malformed legacy data.
ALTER TABLE app_permission
    ALTER COLUMN resource_code SET DEFAULT 'unknown';

ALTER TABLE app_permission
    ALTER COLUMN action_code SET DEFAULT 'unknown';

UPDATE app_permission SET resource_code = 'unknown' WHERE resource_code IS NULL;
UPDATE app_permission SET action_code = 'unknown' WHERE action_code IS NULL;

ALTER TABLE app_permission
    ALTER COLUMN resource_code SET NOT NULL;

ALTER TABLE app_permission
    ALTER COLUMN action_code SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_permission_resource_action
    ON app_permission(resource_code, action_code);
