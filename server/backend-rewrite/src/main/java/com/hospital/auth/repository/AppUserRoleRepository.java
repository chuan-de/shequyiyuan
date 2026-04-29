package com.hospital.auth.repository;

import com.hospital.auth.entity.AppUserRole;
import com.hospital.auth.entity.AppUserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRoleRepository extends JpaRepository<AppUserRole, AppUserRoleId> {

    List<AppUserRole> findByIdUserId(Long userId);
}
