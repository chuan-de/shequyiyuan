package com.hospital.auth.service;

import com.hospital.common.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色 ↔ 权限分配的管理服务（角色权限配置页后端）。
 *
 * <p>权限解析在 {@code JwtAuthenticationFilter} 里每个请求都从数据库读取，
 * 所以这里的改动对在线用户下一次请求即生效，无需重新登录。</p>
 *
 * <p>护栏：ADMIN 角色的权限集固定不可修改 —— 防止管理员把自己锁在
 * 配置页（rbac:write）之外后无人能恢复。</p>
 */
@Service
public class RbacAdminService {

    private final JdbcClient jdbcClient;

    public RbacAdminService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record PermissionItem(Long id, String code, String name) {}
    public record RoleWithPermissions(Long id, String roleCode, String roleName, List<String> permissionCodes) {}

    @Transactional(readOnly = true)
    public List<PermissionItem> listPermissions() {
        return jdbcClient.sql("""
                SELECT id, permission_code, permission_name
                FROM app_permission ORDER BY permission_code
                """)
                .query((rs, n) -> new PermissionItem(
                        rs.getLong("id"), rs.getString("permission_code"), rs.getString("permission_name")))
                .list();
    }

    @Transactional(readOnly = true)
    public List<RoleWithPermissions> listRoles() {
        Map<Long, List<String>> permsByRole = new HashMap<>();
        jdbcClient.sql("""
                SELECT rp.role_id, p.permission_code
                FROM app_role_permission rp
                JOIN app_permission p ON p.id = rp.permission_id
                ORDER BY p.permission_code
                """)
                .query((rs, n) -> {
                    permsByRole.computeIfAbsent(rs.getLong("role_id"), k -> new java.util.ArrayList<>())
                            .add(rs.getString("permission_code"));
                    return null;
                })
                .list();
        return jdbcClient.sql("SELECT id, role_code, role_name FROM app_role ORDER BY id")
                .query((rs, n) -> new RoleWithPermissions(
                        rs.getLong("id"), rs.getString("role_code"), rs.getString("role_name"),
                        permsByRole.getOrDefault(rs.getLong("id"), List.of())))
                .list();
    }

    @Transactional
    public RoleWithPermissions replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        String roleCode = jdbcClient.sql("SELECT role_code FROM app_role WHERE id = :id")
                .param("id", roleId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        if ("ADMIN".equals(roleCode)) {
            throw new IllegalArgumentException("管理员角色的权限固定，不可修改");
        }

        List<String> codes = permissionCodes == null ? List.of() : permissionCodes.stream()
                .filter(c -> c != null && !c.isBlank()).distinct().toList();

        // 校验所有权限码存在，避免静默丢弃拼写错误。
        if (!codes.isEmpty()) {
            long known = jdbcClient.sql("SELECT COUNT(*) FROM app_permission WHERE permission_code IN (:codes)")
                    .param("codes", codes)
                    .query(Long.class).single();
            if (known != codes.size()) {
                throw new IllegalArgumentException("存在未知的权限码，请刷新页面后重试");
            }
        }

        jdbcClient.sql("DELETE FROM app_role_permission WHERE role_id = :id")
                .param("id", roleId).update();
        if (!codes.isEmpty()) {
            jdbcClient.sql("""
                    INSERT INTO app_role_permission (role_id, permission_id)
                    SELECT :roleId, id FROM app_permission WHERE permission_code IN (:codes)
                    """)
                    .param("roleId", roleId)
                    .param("codes", codes)
                    .update();
        }
        return listRoles().stream().filter(r -> r.id().equals(roleId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
    }
}
