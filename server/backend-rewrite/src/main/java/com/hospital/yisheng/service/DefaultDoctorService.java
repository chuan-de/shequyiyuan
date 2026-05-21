package com.hospital.yisheng.service;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import com.hospital.yisheng.dto.DoctorUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.yisheng.repository.DoctorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultDoctorService implements DoctorService {
    private final DoctorRepository repository;

    public DefaultDoctorService(DoctorRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorProfile> list(String keyword, DoctorStatus status) {
        return repository.findAll().stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> keyword == null || keyword.isBlank() ||
                        d.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        d.getDepartment().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorProfile detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Doctor not found"));
    }

    @Override
    public DoctorProfile create(DoctorUpsertRequest request, String actor) {
        return repository.save(new DoctorProfile(null, request.name(), request.department(), DoctorStatus.ACTIVE, request.version()));
    }

    @Override
    public DoctorProfile update(Long id, DoctorUpsertRequest request, String actor) {
        DoctorProfile current = detail(id);
        return repository.save(new DoctorProfile(current.getId(), request.name(), request.department(), current.getStatus(), current.getVersion()));
    }

    @Override
    public DoctorProfile changeStatus(Long id, DoctorStatus status, String actor) {
        DoctorProfile current = detail(id);
        return repository.save(new DoctorProfile(current.getId(), current.getName(), current.getDepartment(), status, current.getVersion()));
    }
}
