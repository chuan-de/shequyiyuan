package com.hospital.yisheng.repository;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDoctorRepository implements DoctorRepository {
    private final ConcurrentHashMap<Long, DoctorProfile> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(2000);

    @Override
    public List<DoctorProfile> findAll(String keyword, DoctorStatus status) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<DoctorProfile> result = new ArrayList<>();
        for (DoctorProfile item : store.values()) {
            boolean keywordMatch = k.isEmpty()
                || item.name().toLowerCase().contains(k)
                || item.department().toLowerCase().contains(k);
            boolean statusMatch = status == null || item.status() == status;
            if (keywordMatch && statusMatch) result.add(item);
        }
        return result;
    }

    @Override
    public Optional<DoctorProfile> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public DoctorProfile save(DoctorProfile doctor) {
        Long id = doctor.id() == null ? seq.incrementAndGet() : doctor.id();
        Long version = doctor.version() == null ? 1L : doctor.version() + 1L;
        DoctorProfile saved = new DoctorProfile(id, doctor.name(), doctor.department(), doctor.status(), version);
        store.put(id, saved);
        return saved;
    }
}
