package com.hospital.yaopin.service;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import com.hospital.yaopin.dto.MedicationUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.yaopin.repository.MedicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultMedicationService implements MedicationService {
    private final MedicationRepository repository;

    public DefaultMedicationService(MedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medication> list(String keyword, MedicationStatus status) {
        return repository.findAll().stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> keyword == null || keyword.isBlank() ||
                        m.getCode().toLowerCase().contains(keyword.toLowerCase()) ||
                        m.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Medication detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Medication not found"));
    }

    @Override
    public Medication create(MedicationUpsertRequest request, String actor) {
        return repository.save(new Medication(null, request.code(), request.name(), MedicationStatus.ENABLED, request.version()));
    }

    @Override
    public Medication update(Long id, MedicationUpsertRequest request, String actor) {
        Medication current = detail(id);
        return repository.save(new Medication(current.getId(), request.code(), request.name(), current.getStatus(), current.getVersion()));
    }

    @Override
    public Medication changeStatus(Long id, MedicationStatus status, String actor) {
        Medication current = detail(id);
        return repository.save(new Medication(current.getId(), current.getCode(), current.getName(), status, current.getVersion()));
    }
}
