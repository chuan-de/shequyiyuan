package com.hospital.auth.repository;

import com.hospital.auth.entity.AppRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByRoleCode(String roleCode);
}
