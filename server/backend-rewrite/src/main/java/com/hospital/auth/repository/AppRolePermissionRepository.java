package com.hospital.auth.repository;

import com.hospital.auth.entity.AppRolePermission;
import com.hospital.auth.entity.AppRolePermissionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppRolePermissionRepository extends JpaRepository<AppRolePermission, AppRolePermissionId> {
    List<AppRolePermission> findByIdRoleId(Long roleId);

    @Query("select rp from AppRolePermission rp join fetch rp.permission where rp.id.roleId in :roleIds")
    List<AppRolePermission> findByIdRoleIdInWithPermission(@Param("roleIds") Collection<Long> roleIds);
}
