package com.hospital.systemconfig.service;

import com.hospital.systemconfig.domain.*;
import com.hospital.systemconfig.dto.*;
import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> list(String key, ConfigStatus status);
    SystemConfig detail(Long id);
    SystemConfig create(SystemConfigUpsertRequest request, String actor);
    SystemConfig update(Long id, SystemConfigUpsertRequest request, String actor);
    SystemConfig changeStatus(Long id, ConfigStatus targetStatus, String actor);
    void delete(Long id, String actor);
}
