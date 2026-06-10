package com.hospital.auth.controller;

import com.hospital.auth.service.RbacAdminService;
import com.hospital.auth.service.RbacAdminService.PermissionItem;
import com.hospital.auth.service.RbacAdminService.RoleWithPermissions;
import com.hospital.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 角色权限配置页接口。改动经 V53 的 rbac:read / rbac:write 控权（默认仅 ADMIN）。 */
@RestController
@RequestMapping("/api/v1/rbac")
public class RbacController {

    private final RbacAdminService service;

    public RbacController(RbacAdminService service) { this.service = service; }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('rbac:read')")
    public ApiResponse<List<PermissionItem>> permissions() {
        return ApiResponse.ok(service.listPermissions());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('rbac:read')")
    public ApiResponse<List<RoleWithPermissions>> roles() {
        return ApiResponse.ok(service.listRoles());
    }

    public record UpdateRolePermissionsRequest(@NotNull List<String> permissionCodes) {}

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('rbac:write')")
    public ApiResponse<RoleWithPermissions> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody UpdateRolePermissionsRequest request) {
        return ApiResponse.ok(service.replaceRolePermissions(roleId, request.permissionCodes()));
    }
}
