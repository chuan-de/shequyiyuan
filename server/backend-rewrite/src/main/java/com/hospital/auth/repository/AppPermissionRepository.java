package com.hospital.auth.repository;

import com.hospital.auth.entity.AppPermission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {
    Optional<AppPermission> findByPermissionCode(String permissionCode);
}
