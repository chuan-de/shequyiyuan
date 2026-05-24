package com.hospital.ai.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hospital.visit.domain.VisitRecord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Slices a {@link VisitRecord} into per-field {@link KnowledgeChunk}s.
 *
 * <p>Visits carry two free-text fields (registration notes + visit content)
 * and a structured department + fee + date that flow into metadata.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class VisitRecordChunker implements Chunker<VisitRecord> {

    @Override
    public List<KnowledgeChunk> chunk(VisitRecord r) {
        if (r == null || r.getPatientId() == null) return List.of();

        Map<String, Object> base = new LinkedHashMap<>();
        base.put("source_created_at", r.getVisitDate() != null ? r.getVisitDate().toString()
                : (r.getCreatedAt() != null ? r.getCreatedAt().toString() : null));
        if (r.getKeshiTypes() != null) base.put("department_code", r.getKeshiTypes());
        if (notBlank(r.getVisitNumber())) base.put("visit_number", r.getVisitNumber());
        if (r.getFee() != null) base.put("fee", r.getFee().toPlainString());

        List<KnowledgeChunk> out = new ArrayList<>();
        addIfPresent(out, r, "registration_notes", r.getRegistrationNotes(), base);
        addIfPresent(out, r, "visit_content", r.getVisitContent(), base);
        return out;
    }

    private void addIfPresent(List<KnowledgeChunk> out,
                              VisitRecord r,
                              String fieldKey,
                              String text,
                              Map<String, Object> baseMetadata) {
        if (!notBlank(text)) return;
        out.add(new KnowledgeChunk(
                KnowledgeChunk.SOURCE_VISIT,
                r.getId(),
                r.getPatientId(),
                fieldKey,
                text.trim(),
                baseMetadata));
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
