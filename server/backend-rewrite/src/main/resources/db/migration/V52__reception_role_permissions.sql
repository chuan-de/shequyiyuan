-- 前台（RECEPTION）角色权限闭环：V22 建了角色但从未分配权限，
-- 前台账号登录后侧边栏为空。挂号与患者建档是前台的本职工作。
INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p
WHERE r.role_code = 'RECEPTION' AND p.permission_code IN (
    'visits:read', 'visits:write',
    'patients:read', 'patients:write',
    'doctors:read',
    'departments:read',
    'family-doctor-contracts:read',
    'followups:read',
    'dictionary:read'
) ON CONFLICT DO NOTHING;
