package com.hospital.medicalrecord.service;

import com.hospital.ai.ingestion.KnowledgeIngestRequestedEvent;
import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.domain.MedicalRecordStatus;
import com.hospital.medicalrecord.dto.MedicalRecordUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.common.PageResponse;
import com.hospital.medicalrecord.repository.MedicalRecordRepository;
import com.hospital.medication.service.MedicationService;
import com.hospital.visit.domain.VisitRecord;
import com.hospital.visit.repository.VisitRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultMedicalRecordService implements MedicalRecordService {
    private final MedicalRecordRepository repository;
    private final MedicationService medicationService;
    private final VisitRepository visitRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultMedicalRecordService(MedicalRecordRepository repository,
                                       MedicationService medicationService,
                                       VisitRepository visitRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.medicationService = medicationService;
        this.visitRepository = visitRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MedicalRecord> list(String keyword, MedicalRecordStatus status, Long patientId, int page, int size) {
        Specification<MedicalRecord> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (patientId != null) predicates.add(cb.equal(root.get("patientId"), patientId));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("caseName")), like),
                        cb.like(cb.lower(root.get("patientName")), like)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        Page<MedicalRecord> result = repository.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResponse<>(result.getContent(), result.getTotalElements(), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Medical record not found"));
    }

    @Override
    public MedicalRecord create(MedicalRecordUpsertRequest r, String actor) {
        validateVisitLink(r.visitId(), r.patientId());
        String caseNumber = (r.caseNumber() == null || r.caseNumber().isBlank())
                ? generateCaseNumber()
                : r.caseNumber();
        MedicalRecord record = new MedicalRecord(null, r.doctorId(), r.doctorUuidNumber(), r.doctorName(),
                r.doctorPhone(), r.doctorIdNumber(), r.doctorEmail(),
                r.patientId(), r.patientName(), r.patientPhone(), r.patientIdNumber(), r.patientEmail(),
                caseNumber, r.caseName(), r.conditionDesc(),
                r.examItems(), r.examResults(),
                r.prescriptionItems(), r.attachments(), r.recordDate(),
                MedicalRecordStatus.DRAFT, r.version());
        record.setVisitId(r.visitId());
        // 同事务内按处方扣减库存（库存不足抛 409，整个保存回滚）。
        applyPrescriptionDelta(null, r.prescriptionItems(), caseNumber, actor);
        MedicalRecord saved = repository.save(record);
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.medicalRecord(saved.getId()));
        return saved;
    }

    @Override
    public MedicalRecord update(Long id, MedicalRecordUpsertRequest r, String actor) {
        MedicalRecord c = detail(id);
        validateVisitLink(r.visitId(), r.patientId());
        MedicalRecord record = new MedicalRecord(c.getId(), r.doctorId(), r.doctorUuidNumber(), r.doctorName(),
                r.doctorPhone(), r.doctorIdNumber(), r.doctorEmail(),
                r.patientId(), r.patientName(), r.patientPhone(), r.patientIdNumber(), r.patientEmail(),
                c.getCaseNumber(), r.caseName(), r.conditionDesc(),
                r.examItems(), r.examResults(),
                r.prescriptionItems(), r.attachments(), r.recordDate(),
                c.getStatus(), c.getVersion());
        record.setVisitId(r.visitId() != null ? r.visitId() : c.getVisitId());
        record.setAiExtracted(c.getAiExtracted());
        // 处方变更按差量调库存：加药扣库存、减药/删药返还库存。
        applyPrescriptionDelta(c.getPrescriptionItems(), r.prescriptionItems(), c.getCaseNumber(), actor);
        MedicalRecord saved = repository.save(record);
        eventPublisher.publishEvent(KnowledgeIngestRequestedEvent.medicalRecord(saved.getId()));
        return saved;
    }

    /** 病历可选挂接就诊记录；挂接时就诊必须存在且属于同一患者。 */
    private void validateVisitLink(Long visitId, Long patientId) {
        if (visitId == null) return;
        VisitRecord visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new NotFoundException("Visit not found: " + visitId));
        if (patientId != null && !Objects.equals(visit.getPatientId(), patientId)) {
            throw new IllegalArgumentException("就诊记录 " + visit.getVisitNumber() + " 不属于所选患者");
        }
    }

    /**
     * 按处方明细差量调整药品库存并记录流水（reason=病历号）。
     * oldItems 传 null 表示新建（全量扣减）。库存不足时由
     * MedicationService 抛出 IllegalArgumentException（HTTP 409），
     * 同事务内病历保存一并回滚。
     */
    private void applyPrescriptionDelta(List<Map<String, Object>> oldItems,
                                        List<Map<String, Object>> newItems,
                                        String caseNumber, String actor) {
        Map<Long, Integer> delta = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        accumulate(newItems, delta, names, +1);
        accumulate(oldItems, delta, names, -1);
        for (Map.Entry<Long, Integer> e : delta.entrySet()) {
            int prescribedChange = e.getValue();
            if (prescribedChange == 0) continue;
            try {
                medicationService.adjustInventory(e.getKey(), -prescribedChange, "处方 " + caseNumber, actor);
            } catch (IllegalArgumentException ex) {
                String name = names.getOrDefault(e.getKey(), "ID " + e.getKey());
                throw new IllegalArgumentException("药品「" + name + "」库存不足，无法开方");
            }
        }
    }

    private static void accumulate(List<Map<String, Object>> items, Map<Long, Integer> delta,
                                   Map<Long, String> names, int sign) {
        if (items == null) return;
        for (Map<String, Object> it : items) {
            Long medId = toLong(it.get("medicationId"));
            Integer qty = toInt(it.get("quantity"));
            if (medId == null || qty == null || qty <= 0) continue;
            delta.merge(medId, sign * qty, Integer::sum);
            Object name = it.get("name");
            if (name != null) names.putIfAbsent(medId, String.valueOf(name));
        }
    }

    private static Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        try { return v != null ? Long.parseLong(String.valueOf(v)) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private static Integer toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try { return v != null ? Integer.parseInt(String.valueOf(v)) : null; }
        catch (NumberFormatException e) { return null; }
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
        MedicalRecord c = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical record not found"));
        // 删除病历返还处方占用的库存。
        applyPrescriptionDelta(c.getPrescriptionItems(), null, c.getCaseNumber(), actor);
        repository.deleteById(id);
    }

    @Override
    public MedicalRecord changeStatus(Long id, MedicalRecordStatus status, String actor) {
        MedicalRecord c = detail(id);
        MedicalRecord record = new MedicalRecord(c.getId(), c.getDoctorId(), c.getDoctorUuidNumber(), c.getDoctorName(),
                c.getDoctorPhone(), c.getDoctorIdNumber(), c.getDoctorEmail(),
                c.getPatientId(), c.getPatientName(), c.getPatientPhone(), c.getPatientIdNumber(), c.getPatientEmail(),
                c.getCaseNumber(), c.getCaseName(), c.getConditionDesc(),
                c.getExamItems(), c.getExamResults(),
                c.getPrescriptionItems(), c.getAttachments(), c.getRecordDate(),
                status, c.getVersion());
        record.setVisitId(c.getVisitId());
        record.setAiExtracted(c.getAiExtracted());
        return repository.save(record);
    }
}
