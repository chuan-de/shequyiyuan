package com.hospital.jiuzhen.service;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import com.hospital.jiuzhen.dto.VisitUpsertRequest;
import com.hospital.jiuzhen.repository.VisitRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultVisitService implements VisitService {
    private final VisitRepository repository;
    public DefaultVisitService(VisitRepository repository) { this.repository = repository; }

    @Override
    public List<VisitRecord> list(String keyword, VisitStatus status) { return repository.findAll(keyword, status); }

    @Override
    public VisitRecord detail(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Visit not found")); }

    @Override
    public VisitRecord create(VisitUpsertRequest request, String actor) {
        return repository.save(new VisitRecord(null, request.patientName(), request.doctorName(), VisitStatus.PENDING, request.version()));
    }

    @Override
    public VisitRecord update(Long id, VisitUpsertRequest request, String actor) {
        VisitRecord current = detail(id);
        return repository.save(new VisitRecord(current.id(), request.patientName(), request.doctorName(), current.status(), current.version()));
    }

    @Override
    public VisitRecord changeStatus(Long id, VisitStatus status, String actor) {
        VisitRecord current = detail(id);
        return repository.save(new VisitRecord(current.id(), current.patientName(), current.doctorName(), status, current.version()));
    }
}
