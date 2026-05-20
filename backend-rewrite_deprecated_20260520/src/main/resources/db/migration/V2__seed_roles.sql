INSERT INTO app_role (role_code, role_name)
VALUES
    ('ADMIN', 'Administrator'),
    ('USER', 'User')
ON CONFLICT (role_code) DO NOTHING;
