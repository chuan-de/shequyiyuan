package com.hospital.jiuzhen.service;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import com.hospital.jiuzhen.dto.VisitUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.jiuzhen.repository.VisitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultVisitService implements VisitService {
    private final VisitRepository repository;

    public DefaultVisitService(VisitRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public List<VisitRecord> list(String keyword, VisitStatus status) {
        return repository.findAll().stream()
                .filter(v -> status == null || v.getStatus() == status)
                .filter(v -> keyword == null || keyword.isBlank() ||
                        v.getPatientName().toLowerCase().contains(keyword.toLowerCase()) ||
                        v.getDoctorName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Visit not found"));
    }

    @Override
    public VisitRecord create(VisitUpsertRequest request, String actor) {
        return repository.save(new VisitRecord(null, request.patientName(), request.doctorName(), VisitStatus.PENDING, request.version()));
    }

    @Override
    public VisitRecord update(Long id, VisitUpsertRequest request, String actor) {
        VisitRecord current = detail(id);
        return repository.save(new VisitRecord(current.getId(), request.patientName(), request.doctorName(), current.getStatus(), current.getVersion()));
    }

    @Override
    public VisitRecord changeStatus(Long id, VisitStatus status, String actor) {
        VisitRecord current = detail(id);
        return repository.save(new VisitRecord(current.getId(), current.getPatientName(), current.getDoctorName(), status, current.getVersion()));
    }
}
