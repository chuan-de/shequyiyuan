package com.hospital.yisheng.service;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import com.hospital.yisheng.dto.DoctorUpsertRequest;
import com.hospital.yisheng.repository.DoctorRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultDoctorService implements DoctorService {
    private final DoctorRepository repository;
    public DefaultDoctorService(DoctorRepository repository) { this.repository = repository; }

    @Override
    public List<DoctorProfile> list(String keyword, DoctorStatus status) { return repository.findAll(keyword, status); }

    @Override
    public DoctorProfile detail(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Doctor not found")); }

    @Override
    public DoctorProfile create(DoctorUpsertRequest request, String actor) {
        return repository.save(new DoctorProfile(null, request.name(), request.department(), DoctorStatus.ACTIVE, request.version()));
    }

    @Override
    public DoctorProfile update(Long id, DoctorUpsertRequest request, String actor) {
        DoctorProfile current = detail(id);
        return repository.save(new DoctorProfile(current.id(), request.name(), request.department(), current.status(), current.version()));
    }

    @Override
    public DoctorProfile changeStatus(Long id, DoctorStatus status, String actor) {
        DoctorProfile current = detail(id);
        return repository.save(new DoctorProfile(current.id(), current.name(), current.department(), status, current.version()));
    }
}
