package com.hospital.configmodule.repository;

import com.hospital.configmodule.domain.ConfigStatus;
import com.hospital.configmodule.domain.SystemConfig;
import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository {
    List<SystemConfig> findAll(String key, ConfigStatus status);
    Optional<SystemConfig> findById(Long id);
    SystemConfig save(SystemConfig record);
}
