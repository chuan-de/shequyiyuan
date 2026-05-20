package com.hospital.jiatingyisheng.service;

import com.hospital.jiatingyisheng.domain.FamilyDoctorContract;
import com.hospital.jiatingyisheng.domain.FamilyDoctorStatus;
import com.hospital.jiatingyisheng.dto.FamilyDoctorUpsertRequest;
import com.hospital.jiatingyisheng.repository.FamilyDoctorRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultFamilyDoctorService implements FamilyDoctorService {
    private final FamilyDoctorRepository repository;

    public DefaultFamilyDoctorService(FamilyDoctorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<FamilyDoctorContract> list(String keyword, FamilyDoctorStatus status) {
        return repository.findAll(keyword, status);
    }

    @Override
    public FamilyDoctorContract detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Family doctor contract not found"));
    }

    @Override
    public FamilyDoctorContract create(FamilyDoctorUpsertRequest request, String actor) {
        return repository.save(new FamilyDoctorContract(null, request.residentId(), request.doctorId(), FamilyDoctorStatus.PENDING, 1L));
    }

    @Override
    public FamilyDoctorContract update(Long id, FamilyDoctorUpsertRequest request, String actor) {
        FamilyDoctorContract current = detail(id);
        return repository.save(new FamilyDoctorContract(current.id(), request.residentId(), request.doctorId(), current.status(), current.version()));
    }

    @Override
    public FamilyDoctorContract changeStatus(Long id, FamilyDoctorStatus targetStatus, String reason, String actor) {
        FamilyDoctorContract current = detail(id);
        return repository.save(new FamilyDoctorContract(current.id(), current.residentId(), current.doctorId(), targetStatus, current.version()));
    }
}
