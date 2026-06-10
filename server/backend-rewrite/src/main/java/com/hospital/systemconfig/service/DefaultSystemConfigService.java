package com.hospital.systemconfig.service;

import com.hospital.systemconfig.domain.ConfigStatus;
import com.hospital.systemconfig.domain.SystemConfig;
import com.hospital.systemconfig.dto.SystemConfigUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.systemconfig.repository.SystemConfigRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSystemConfigService implements SystemConfigService {

    /**
     * 可被任意登录用户读取的运行参数白名单。新增业务配置时在这里登记；
     * 不在白名单内的 key 永远不会通过 /effective 暴露出去。
     */
    private static final Set<String> EFFECTIVE_KEY_WHITELIST = Set.of(
        "system.announcement",            // 首页公告
        "medication.low-stock-threshold", // 药品低库存预警阈值
        "visit.default-fee"               // 新建就诊默认挂号费
    );

    private final SystemConfigRepository repository;

    public DefaultSystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> list(String key, ConfigStatus status) {
        return repository.findAll().stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> key == null || key.isBlank() ||
                        c.getConfigKey().toLowerCase().contains(key.toLowerCase()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> effectiveConfigs() {
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfig config : repository.findAll()) {
            if (config.getStatus() == ConfigStatus.ENABLED && EFFECTIVE_KEY_WHITELIST.contains(config.getConfigKey())) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemConfig detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("SystemConfig not found"));
    }

    @Override
    public SystemConfig create(SystemConfigUpsertRequest request, String actor) {
        return repository.save(new SystemConfig(null, request.configKey(), request.configValue(), ConfigStatus.ENABLED, request.version()));
    }

    @Override
    public SystemConfig update(Long id, SystemConfigUpsertRequest request, String actor) {
        SystemConfig current = detail(id);
        return repository.save(new SystemConfig(current.getId(), request.configKey(), request.configValue(), current.getStatus(), current.getVersion()));
    }

    @Override
    public SystemConfig changeStatus(Long id, ConfigStatus targetStatus, String actor) {
        SystemConfig current = detail(id);
        return repository.save(new SystemConfig(current.getId(), current.getConfigKey(), current.getConfigValue(), targetStatus, current.getVersion()));
    }

    @Override
    public void delete(Long id, String actor) {
        if (!repository.existsById(id)) throw new NotFoundException("SystemConfig not found");
        repository.deleteById(id);
    }
}
