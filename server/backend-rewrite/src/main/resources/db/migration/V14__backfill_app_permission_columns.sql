-- Ensure app_permission has resource_code/action_code for both legacy and fresh environments.
-- Safe for already-migrated databases because all operations are idempotent.
ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS resource_code VARCHAR(50);

ALTER TABLE app_permission
    ADD COLUMN IF NOT EXISTS action_code VARCHAR(50);

-- Backfill historical rows from permission_code.
-- resource_code: first segment before ':'
-- action_code: last segment after ':'
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

-- Protect against malformed historical data and enforce NOT NULL semantics.
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
