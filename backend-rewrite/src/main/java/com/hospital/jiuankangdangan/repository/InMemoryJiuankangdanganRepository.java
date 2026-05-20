package com.hospital.jiuankangdangan.repository;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryJiuankangdanganRepository implements JiuankangdanganRepository {
    private final ConcurrentHashMap<Long, JiuankangdanganRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(3000);

    @Override
    public List<JiuankangdanganRecord> findAll(String keyword, JiuankangdanganStatus status) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<JiuankangdanganRecord> result = new ArrayList<>();
        for (JiuankangdanganRecord item : store.values()) {
            boolean keywordMatch = k.isEmpty() || item.jiuankangdanganName().toLowerCase().contains(k) || item.yonghuName().toLowerCase().contains(k);
            boolean statusMatch = status == null || item.status() == status;
            if (keywordMatch && statusMatch) result.add(item);
        }
        return result;
    }

    @Override public Optional<JiuankangdanganRecord> findById(Long id) { return Optional.ofNullable(store.get(id)); }

    @Override public JiuankangdanganRecord save(JiuankangdanganRecord r) {
        Long id = r.id() == null ? seq.incrementAndGet() : r.id();
        Long version = r.version() == null ? 1L : r.version() + 1L;
        JiuankangdanganRecord saved = new JiuankangdanganRecord(id, r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(),
                r.yonghuEmail(), r.jiuankangdanganName(), r.jiuankangdanganQita(), r.jiuankangdanganTypes(), r.insertTime(),
                r.jiuankangdanganContent(), r.status(), version);
        store.put(id, saved);
        return saved;
    }
}
