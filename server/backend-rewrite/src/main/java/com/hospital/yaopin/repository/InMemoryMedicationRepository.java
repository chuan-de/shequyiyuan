package com.hospital.yaopin.repository;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMedicationRepository implements MedicationRepository {
    private final Map<Long, Medication> data = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public List<Medication> findAll(String keyword, MedicationStatus status) {
        List<Medication> rows = new ArrayList<>(data.values());
        if (status != null) {
            rows = rows.stream().filter(row -> row.status() == status).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.trim().toLowerCase();
            rows = rows.stream().filter(row -> row.code().toLowerCase().contains(key) || row.name().toLowerCase().contains(key)).toList();
        }
        return rows;
    }

    @Override
    public Optional<Medication> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Medication save(Medication medication) {
        Long id = medication.id() == null ? seq.getAndIncrement() : medication.id();
        Medication saved = new Medication(id, medication.code(), medication.name(), medication.status(), medication.version());
        data.put(id, saved);
        return saved;
    }
}
