package com.hospital.bingli.repository;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBingliRepository implements BingliRepository {
    private final ConcurrentHashMap<Long, BingliRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(2000);

    @Override
    public List<BingliRecord> findAll(String keyword, BingliStatus status) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<BingliRecord> result = new ArrayList<>();
        for (BingliRecord item : store.values()) {
            boolean keywordMatch = k.isEmpty() || item.bingliName().toLowerCase().contains(k)
                    || item.yishengName().toLowerCase().contains(k) || item.yonghuName().toLowerCase().contains(k);
            boolean statusMatch = status == null || item.status() == status;
            if (keywordMatch && statusMatch) result.add(item);
        }
        return result;
    }

    @Override
    public Optional<BingliRecord> findById(Long id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public BingliRecord save(BingliRecord record) {
        Long id = record.id() == null ? seq.incrementAndGet() : record.id();
        Long version = record.version() == null ? 1L : record.version() + 1L;
        BingliRecord saved = new BingliRecord(id, record.yishengId(), record.yishengUuidNumber(), record.yishengName(), record.yishengPhone(),
                record.yishengIdNumber(), record.yishengEmail(), record.yonghuId(), record.yonghuName(), record.yonghuPhone(),
                record.yonghuIdNumber(), record.yonghuEmail(), record.bingliUuidNumber(), record.bingliName(), record.bingliBingqing(),
                record.jianchaxiangmu(), record.jianchajieguo(), record.status(), version);
        store.put(id, saved);
        return saved;
    }
}
