package com.hospital.configmodule.service;

import com.hospital.configmodule.domain.*;
import com.hospital.configmodule.dto.*;
import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> list(String key, ConfigStatus status);
    SystemConfig detail(Long id);
    SystemConfig create(SystemConfigUpsertRequest request, String actor);
    SystemConfig update(Long id, SystemConfigUpsertRequest request, String actor);
    SystemConfig changeStatus(Long id, ConfigStatus targetStatus, String actor);
}
