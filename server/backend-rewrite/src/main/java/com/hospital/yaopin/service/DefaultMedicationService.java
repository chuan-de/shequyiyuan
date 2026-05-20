package com.hospital.yaopin.service;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import com.hospital.yaopin.dto.MedicationUpsertRequest;
import com.hospital.yaopin.repository.MedicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultMedicationService implements MedicationService {
    private final MedicationRepository repository;

    public DefaultMedicationService(MedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Medication> list(String keyword, MedicationStatus status) {
        return repository.findAll(keyword, status);
    }

    @Override
    public Medication detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Medication not found"));
    }

    @Override
    public Medication create(MedicationUpsertRequest request, String actor) {
        return repository.save(new Medication(null, request.code(), request.name(), MedicationStatus.ENABLED, request.version()));
    }

    @Override
    public Medication update(Long id, MedicationUpsertRequest request, String actor) {
        Medication current = detail(id);
        return repository.save(new Medication(current.id(), request.code(), request.name(), current.status(), current.version()));
    }

    @Override
    public Medication changeStatus(Long id, MedicationStatus status, String actor) {
        Medication current = detail(id);
        return repository.save(new Medication(current.id(), current.code(), current.name(), status, current.version()));
    }
}
