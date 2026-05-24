package com.hospital.ai.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hospital.medicalrecord.domain.MedicalRecord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Slices a {@link MedicalRecord} into per-field {@link KnowledgeChunk}s.
 *
 * <p>Field mapping (matches docs/ai-features-plan.md Phase 2.5):</p>
 * <ul>
 *   <li>{@code chief_complaint} ← {@code conditionDesc}</li>
 *   <li>{@code diagnosis}      ← {@code examResults} / {@code caseName}</li>
 *   <li>{@code prescription}    ← {@code prescriptionItems} rendered as
 *       "name × qty, name × qty"</li>
 *   <li>{@code exam}           ← {@code examItems}</li>
 *   <li>{@code ai_extracted}    ← stringified ai_extracted JSON (so OCR'd
 *       fields are reachable even if a medic forgot to copy them out)</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class MedicalRecordChunker implements Chunker<MedicalRecord> {

    @Override
    public List<KnowledgeChunk> chunk(MedicalRecord r) {
        if (r == null || r.getPatientId() == null) return List.of();

        Map<String, Object> base = baseMetadata(r);
        List<KnowledgeChunk> out = new ArrayList<>();

        addIfPresent(out, r, "chief_complaint", r.getConditionDesc(), base);

        // diagnosis: prefer examResults; fall back to caseName when results
        // are blank (early-stage drafts often only have the diagnosis name).
        String diagnosis = notBlank(r.getExamResults()) ? r.getExamResults() : r.getCaseName();
        addIfPresent(out, r, "diagnosis", diagnosis, base);

        String prescription = renderPrescription(r.getPrescriptionItems());
        addIfPresent(out, r, "prescription", prescription, base);

        addIfPresent(out, r, "exam", r.getExamItems(), base);

        String aiExtracted = renderAiExtracted(r.getAiExtracted());
        addIfPresent(out, r, "ai_extracted", aiExtracted, base);

        return out;
    }

    private Map<String, Object> baseMetadata(MedicalRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("source_created_at", r.getRecordDate() != null ? r.getRecordDate().toString()
                : (r.getCreatedAt() != null ? r.getCreatedAt().toString() : null));
        if (r.getDoctorId() != null) m.put("doctor_id", r.getDoctorId());
        if (notBlank(r.getDoctorName())) m.put("doctor_name", r.getDoctorName());
        if (notBlank(r.getCaseNumber())) m.put("case_number", r.getCaseNumber());
        if (notBlank(r.getCaseName())) m.put("case_name", r.getCaseName());
        return m;
    }

    private void addIfPresent(List<KnowledgeChunk> out,
                              MedicalRecord r,
                              String fieldKey,
                              String text,
                              Map<String, Object> baseMetadata) {
        if (!notBlank(text)) return;
        out.add(new KnowledgeChunk(
                KnowledgeChunk.SOURCE_MEDICAL_RECORD,
                r.getId(),
                r.getPatientId(),
                fieldKey,
                text.trim(),
                baseMetadata));
    }

    private static String renderPrescription(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            if (item == null) continue;
            Object name = firstNonNull(item, "name", "medicationName", "medicineName", "drug");
            Object qty = firstNonNull(item, "quantity", "qty", "amount", "dose");
            Object unit = firstNonNull(item, "unit", "unitName");
            if (name == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
            if (qty != null) {
                sb.append(" × ").append(qty);
                if (unit != null) sb.append(unit);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Best-effort human-readable rendering of the OCR'd JSON. */
    static String renderAiExtracted(Map<String, Object> aiExtracted) {
        if (aiExtracted == null || aiExtracted.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : aiExtracted.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value == null) continue;
            String text = value.toString().trim();
            if (text.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(key).append(": ").append(text);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static Object firstNonNull(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !String.valueOf(v).isBlank()) return v;
        }
        return null;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
