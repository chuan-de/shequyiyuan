package com.hospital.systemconfig.service;

import com.hospital.systemconfig.domain.*;
import com.hospital.systemconfig.dto.*;
import java.util.List;
import java.util.Map;

public interface SystemConfigService {
    List<SystemConfig> list(String key, ConfigStatus status);
    /** 业务运行参数（白名单内、ENABLED 状态），任何登录用户可读。 */
    Map<String, String> effectiveConfigs();
    SystemConfig detail(Long id);
    SystemConfig create(SystemConfigUpsertRequest request, String actor);
    SystemConfig update(Long id, SystemConfigUpsertRequest request, String actor);
    SystemConfig changeStatus(Long id, ConfigStatus targetStatus, String actor);
    void delete(Long id, String actor);
}
