package com.hospital.auth.repository;

import com.hospital.auth.entity.AppUserRole;
import com.hospital.auth.entity.AppUserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRoleRepository extends JpaRepository<AppUserRole, AppUserRoleId> {

    List<AppUserRole> findByIdUserId(Long userId);

    @Query("select link from AppUserRole link join fetch link.role where link.id.userId = :userId")
    List<AppUserRole> findByIdUserIdWithRole(@Param("userId") Long userId);
}
