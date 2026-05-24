package com.hospital.healthrecord.service;

import com.hospital.healthrecord.domain.HealthRecord;
import com.hospital.healthrecord.domain.HealthRecordStatus;
import com.hospital.healthrecord.dto.HealthRecordUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.healthrecord.repository.HealthRecordRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultHealthRecordService implements HealthRecordService {
    private final HealthRecordRepository repository;

    public DefaultHealthRecordService(HealthRecordRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public List<HealthRecord> list(String keyword, HealthRecordStatus status) {
        return repository.findAll().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> keyword == null || keyword.isBlank() ||
                        (r.getPatientName() != null && r.getPatientName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (r.getTitle() != null && r.getTitle().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HealthRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Health record not found"));
    }

    @Override
    public HealthRecord create(HealthRecordUpsertRequest r, String actor) {
        return repository.save(new HealthRecord(null, r.patientId(), r.patientName(), r.patientPhone(),
                r.patientIdNumber(), r.patientEmail(), r.title(), r.otherMembers(),
                r.unitTypes(), r.recordedAt() != null ? r.recordedAt() : Instant.now(),
                r.content(), HealthRecordStatus.DRAFT, r.version()));
    }

    @Override
    public HealthRecord update(Long id, HealthRecordUpsertRequest r, String actor) {
        HealthRecord c = detail(id);
        return repository.save(new HealthRecord(c.getId(), r.patientId(), r.patientName(), r.patientPhone(),
                r.patientIdNumber(), r.patientEmail(), r.title(), r.otherMembers(),
                r.unitTypes(), r.recordedAt() != null ? r.recordedAt() : c.getRecordedAt(),
                r.content(), c.getStatus(), c.getVersion()));
    }

    @Override
    public void delete(Long id, String actor) {
        if (!repository.existsById(id)) throw new NotFoundException("Health record not found");
        repository.deleteById(id);
    }

    @Override
    public HealthRecord changeStatus(Long id, HealthRecordStatus status, String actor) {
        HealthRecord c = detail(id);
        return repository.save(new HealthRecord(c.getId(), c.getPatientId(), c.getPatientName(), c.getPatientPhone(),
                c.getPatientIdNumber(), c.getPatientEmail(), c.getTitle(), c.getOtherMembers(),
                c.getUnitTypes(), c.getRecordedAt(), c.getContent(), status, c.getVersion()));
    }
}
