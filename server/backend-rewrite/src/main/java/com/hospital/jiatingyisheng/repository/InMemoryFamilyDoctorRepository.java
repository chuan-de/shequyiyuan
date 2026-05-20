package com.hospital.jiatingyisheng.repository;

import com.hospital.jiatingyisheng.domain.FamilyDoctorContract;
import com.hospital.jiatingyisheng.domain.FamilyDoctorStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFamilyDoctorRepository implements FamilyDoctorRepository {
    private final Map<Long, FamilyDoctorContract> data = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public List<FamilyDoctorContract> findAll(String keyword, FamilyDoctorStatus status) {
        List<FamilyDoctorContract> rows = new ArrayList<>(data.values());
        if (status != null) {
            rows = rows.stream().filter(row -> row.status() == status).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.trim();
            rows = rows.stream()
                    .filter(row -> String.valueOf(row.residentId()).contains(key) || String.valueOf(row.doctorId()).contains(key))
                    .toList();
        }
        return rows;
    }

    @Override
    public Optional<FamilyDoctorContract> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public FamilyDoctorContract save(FamilyDoctorContract contract) {
        Long id = contract.id() == null ? seq.getAndIncrement() : contract.id();
        FamilyDoctorContract saved = new FamilyDoctorContract(id, contract.residentId(), contract.doctorId(), contract.status(), contract.version());
        data.put(id, saved);
        return saved;
    }
}
