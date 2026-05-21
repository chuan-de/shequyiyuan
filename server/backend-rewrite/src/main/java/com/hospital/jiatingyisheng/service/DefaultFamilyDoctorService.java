package com.hospital.jiatingyisheng.service;

import com.hospital.jiatingyisheng.domain.FamilyDoctorContract;
import com.hospital.jiatingyisheng.domain.FamilyDoctorStatus;
import com.hospital.jiatingyisheng.dto.FamilyDoctorUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.jiatingyisheng.repository.FamilyDoctorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultFamilyDoctorService implements FamilyDoctorService {
    private final FamilyDoctorRepository repository;

    public DefaultFamilyDoctorService(FamilyDoctorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyDoctorContract> list(String keyword, FamilyDoctorStatus status) {
        return repository.findAll().stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> keyword == null || keyword.isBlank() ||
                        String.valueOf(c.getResidentId()).contains(keyword) ||
                        String.valueOf(c.getDoctorId()).contains(keyword))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyDoctorContract detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Family doctor contract not found"));
    }

    @Override
    public FamilyDoctorContract create(FamilyDoctorUpsertRequest request, String actor) {
        return repository.save(new FamilyDoctorContract(null, request.residentId(), request.doctorId(), FamilyDoctorStatus.PENDING, 0L));
    }

    @Override
    public FamilyDoctorContract update(Long id, FamilyDoctorUpsertRequest request, String actor) {
        FamilyDoctorContract current = detail(id);
        return repository.save(new FamilyDoctorContract(current.getId(), request.residentId(), request.doctorId(), current.getStatus(), current.getVersion()));
    }

    @Override
    public FamilyDoctorContract changeStatus(Long id, FamilyDoctorStatus targetStatus, String reason, String actor) {
        FamilyDoctorContract current = detail(id);
        return repository.save(new FamilyDoctorContract(current.getId(), current.getResidentId(), current.getDoctorId(), targetStatus, current.getVersion()));
    }
}
