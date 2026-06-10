package com.hospital.healthrecord.service;

import com.hospital.ai.ingestion.KnowledgeIngestRequestedEvent;
import com.hospital.healthrecord.domain.HealthRecord;
import com.hospital.healthrecord.domain.HealthRecordStatus;
import com.hospital.healthrecord.dto.HealthRecordUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.healthrecord.repository.HealthRecordRepository;
import com.hospital.patient.domain.PatientProfile;
import com.hospital.patient.repository.PatientProfileRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultHealthRecordService implements HealthRecordService {
    private final HealthRecordRepository repository;
    private final PatientProfileRepository patientProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultHealthRecordService(HealthRecordRepository repository,
                                      PatientProfileRepository patientProfileRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.patientProfileRepository = patientProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthRecord> list(String keyword, HealthRecordStatus status, String patientName, Integer unitTypes) {
        return repository.findAll().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> unitTypes == null || unitTypes.equals(r.getUnitTypes()))
                .filter(r -> patientName == null || patientName.isBlank() ||
                        (r.getPatientName() != null && r.getPatientName().toLowerCase().contains(patientName.toLowerCase())))
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
        PatientProfile patient = resolvePatient(r.patientId());
        HealthRecord saved = repository.save(new HealthRecord(null, patient.getId(), patient.getFullName(), patient.getPhone(),
                patient.getIdNumber(), patient.getEmail(), r.title(), r.otherMembers(),
                r.unitTypes(), r.recordedAt() != null ? r.recordedAt() : Instant.now(),
                r.content(), HealthRecordStatus.DRAFT, r.version()));
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.healthRecord(saved.getId()));
        return saved;
    }

    @Override
    public HealthRecord update(Long id, HealthRecordUpsertRequest r, String actor) {
        HealthRecord c = detail(id);
        PatientProfile patient = resolvePatient(r.patientId() != null ? r.patientId() : c.getPatientId());
        HealthRecord saved = repository.save(new HealthRecord(c.getId(), patient.getId(), patient.getFullName(), patient.getPhone(),
                patient.getIdNumber(), patient.getEmail(), r.title(), r.otherMembers(),
                r.unitTypes(), r.recordedAt() != null ? r.recordedAt() : c.getRecordedAt(),
                r.content(), c.getStatus(), c.getVersion()));
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.healthRecord(saved.getId()));
        return saved;
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

    /** 患者信息以 patient_profile 为准回填冗余列，客户端传的姓名/电话等一律忽略。 */
    private PatientProfile resolvePatient(Long patientId) {
        return patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
    }
}
