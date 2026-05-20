package com.hospital.configmodule.repository;

import com.hospital.configmodule.domain.ConfigStatus;
import com.hospital.configmodule.domain.SystemConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySystemConfigRepository implements SystemConfigRepository {
    private final ConcurrentHashMap<Long, SystemConfig> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(3000);

    @Override
    public List<SystemConfig> findAll(String key, ConfigStatus status) {
        String normalizedKey = key == null ? "" : key.trim().toLowerCase();
        List<SystemConfig> result = new ArrayList<>();
        for (SystemConfig item : store.values()) {
            boolean keyMatch = normalizedKey.isEmpty() || item.configKey().toLowerCase().contains(normalizedKey);
            boolean statusMatch = status == null || item.status() == status;
            if (keyMatch && statusMatch) result.add(item);
        }
        return result;
    }

    @Override
    public Optional<SystemConfig> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public SystemConfig save(SystemConfig record) {
        Long id = record.id() == null ? seq.incrementAndGet() : record.id();
        Long version = record.version() == null ? 1L : record.version() + 1L;
        SystemConfig saved = new SystemConfig(id, record.configKey(), record.configValue(), record.status(), version);
        store.put(id, saved);
        return saved;
    }
}
