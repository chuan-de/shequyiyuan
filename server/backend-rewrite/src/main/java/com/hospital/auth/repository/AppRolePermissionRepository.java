package com.hospital.auth.repository;

import com.hospital.auth.entity.AppRolePermission;
import com.hospital.auth.entity.AppRolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRolePermissionRepository extends JpaRepository<AppRolePermission, AppRolePermissionId> {
    List<AppRolePermission> findByIdRoleId(Long roleId);
}
