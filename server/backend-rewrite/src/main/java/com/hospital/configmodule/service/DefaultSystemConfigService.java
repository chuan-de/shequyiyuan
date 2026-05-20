package com.hospital.configmodule.service;

import com.hospital.configmodule.domain.ConfigStatus;
import com.hospital.configmodule.domain.SystemConfig;
import com.hospital.configmodule.dto.SystemConfigUpsertRequest;
import com.hospital.configmodule.repository.SystemConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultSystemConfigService implements SystemConfigService {
    private final SystemConfigRepository repository;

    public DefaultSystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SystemConfig> list(String key, ConfigStatus status) {
        return repository.findAll(key, status);
    }

    @Override
    public SystemConfig detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("SystemConfig not found"));
    }

    @Override
    public SystemConfig create(SystemConfigUpsertRequest request, String actor) {
        return repository.save(new SystemConfig(null, request.configKey(), request.configValue(), ConfigStatus.ENABLED, request.version()));
    }

    @Override
    public SystemConfig update(Long id, SystemConfigUpsertRequest request, String actor) {
        SystemConfig current = detail(id);
        return repository.save(new SystemConfig(current.id(), request.configKey(), request.configValue(), current.status(), current.version()));
    }

    @Override
    public SystemConfig changeStatus(Long id, ConfigStatus targetStatus, String actor) {
        SystemConfig current = detail(id);
        return repository.save(new SystemConfig(current.id(), current.configKey(), current.configValue(), targetStatus, current.version()));
    }
}
