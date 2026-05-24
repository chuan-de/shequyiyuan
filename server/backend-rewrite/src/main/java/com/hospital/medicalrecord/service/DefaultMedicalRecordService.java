package com.hospital.medicalrecord.service;

import com.hospital.ai.ingestion.KnowledgeIngestRequestedEvent;
import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.domain.MedicalRecordStatus;
import com.hospital.medicalrecord.dto.MedicalRecordUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.medicalrecord.repository.MedicalRecordRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultMedicalRecordService implements MedicalRecordService {
    private final MedicalRecordRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultMedicalRecordService(MedicalRecordRepository repository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecord> list(String keyword, MedicalRecordStatus status) {
        return repository.findAll().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> keyword == null || keyword.isBlank() ||
                        (b.getCaseName() != null && b.getCaseName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (b.getPatientName() != null && b.getPatientName().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Medical record not found"));
    }

    @Override
    public MedicalRecord create(MedicalRecordUpsertRequest r, String actor) {
        String caseNumber = (r.caseNumber() == null || r.caseNumber().isBlank())
                ? generateCaseNumber()
                : r.caseNumber();
        MedicalRecord saved = repository.save(new MedicalRecord(null, r.doctorId(), r.doctorUuidNumber(), r.doctorName(),
                r.doctorPhone(), r.doctorIdNumber(), r.doctorEmail(),
                r.patientId(), r.patientName(), r.patientPhone(), r.patientIdNumber(), r.patientEmail(),
                caseNumber, r.caseName(), r.conditionDesc(),
                r.examItems(), r.examResults(),
                r.prescriptionItems(), r.attachments(), r.recordDate(),
                MedicalRecordStatus.DRAFT, r.version()));
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.medicalRecord(saved.getId()));
        return saved;
    }

    @Override
    public MedicalRecord update(Long id, MedicalRecordUpsertRequest r, String actor) {
        MedicalRecord c = detail(id);
        MedicalRecord saved = repository.save(new MedicalRecord(c.getId(), r.doctorId(), r.doctorUuidNumber(), r.doctorName(),
                r.doctorPhone(), r.doctorIdNumber(), r.doctorEmail(),
                r.patientId(), r.patientName(), r.patientPhone(), r.patientIdNumber(), r.patientEmail(),
                c.getCaseNumber(), r.caseName(), r.conditionDesc(),
                r.examItems(), r.examResults(),
                r.prescriptionItems(), r.attachments(), r.recordDate(),
                c.getStatus(), c.getVersion()));
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.medicalRecord(saved.getId()));
        return saved;
    }

    private String generateCaseNumber() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = (int) (Math.random() * 900 + 100);
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "BL" + stamp + suffix;
            if (!repository.existsByCaseNumber(candidate)) return candidate;
            suffix++;
        }
        throw new IllegalStateException("Failed to generate unique case number");
    }

    @Override
    public void delete(Long id, String actor) {
        if (!repository.existsById(id)) throw new NotFoundException("Medical record not found");
        repository.deleteById(id);
    }

    @Override
    public MedicalRecord changeStatus(Long id, MedicalRecordStatus status, String actor) {
        MedicalRecord c = detail(id);
        return repository.save(new MedicalRecord(c.getId(), c.getDoctorId(), c.getDoctorUuidNumber(), c.getDoctorName(),
                c.getDoctorPhone(), c.getDoctorIdNumber(), c.getDoctorEmail(),
                c.getPatientId(), c.getPatientName(), c.getPatientPhone(), c.getPatientIdNumber(), c.getPatientEmail(),
                c.getCaseNumber(), c.getCaseName(), c.getConditionDesc(),
                c.getExamItems(), c.getExamResults(),
                c.getPrescriptionItems(), c.getAttachments(), c.getRecordDate(),
                status, c.getVersion()));
    }
}
