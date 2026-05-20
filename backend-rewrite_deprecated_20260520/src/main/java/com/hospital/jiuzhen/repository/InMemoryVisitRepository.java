package com.hospital.jiuzhen.repository;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryVisitRepository implements VisitRepository {
    private final ConcurrentHashMap<Long, VisitRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1000);

    @Override
    public List<VisitRecord> findAll(String keyword, VisitStatus status) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<VisitRecord> result = new ArrayList<>();
        for (VisitRecord item : store.values()) {
            boolean keywordMatch = k.isEmpty()
                || item.patientName().toLowerCase().contains(k)
                || item.doctorName().toLowerCase().contains(k);
            boolean statusMatch = status == null || item.status() == status;
            if (keywordMatch && statusMatch) result.add(item);
        }
        return result;
    }

    @Override
    public Optional<VisitRecord> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public VisitRecord save(VisitRecord visit) {
        Long id = visit.id() == null ? seq.incrementAndGet() : visit.id();
        Long version = visit.version() == null ? 1L : visit.version() + 1L;
        VisitRecord saved = new VisitRecord(id, visit.patientName(), visit.doctorName(), visit.status(), version);
        store.put(id, saved);
        return saved;
    }
}
