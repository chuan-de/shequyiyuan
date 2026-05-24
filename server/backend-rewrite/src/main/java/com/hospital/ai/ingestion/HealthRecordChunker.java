package com.hospital.ai.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hospital.healthrecord.domain.HealthRecord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Slices a {@link HealthRecord} into per-field {@link KnowledgeChunk}s.
 *
 * <p>Health records have a free-text {@code content} field plus a title.
 * We chunk them separately so retrieval can surface either the headline or
 * the body. {@code other_members} (family) is rarely informative on its own
 * so it goes into the metadata, not its own chunk.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class HealthRecordChunker implements Chunker<HealthRecord> {

    @Override
    public List<KnowledgeChunk> chunk(HealthRecord r) {
        if (r == null || r.getPatientId() == null) return List.of();

        Map<String, Object> base = new LinkedHashMap<>();
        base.put("source_created_at", r.getRecordedAt() != null ? r.getRecordedAt().toString()
                : (r.getCreatedAt() != null ? r.getCreatedAt().toString() : null));
        if (notBlank(r.getOtherMembers())) base.put("other_members", r.getOtherMembers());
        if (r.getUnitTypes() != null) base.put("unit_types", r.getUnitTypes());

        List<KnowledgeChunk> out = new ArrayList<>();
        addIfPresent(out, r, "title", r.getTitle(), base);
        addIfPresent(out, r, "content", r.getContent(), base);
        return out;
    }

    private void addIfPresent(List<KnowledgeChunk> out,
                              HealthRecord r,
                              String fieldKey,
                              String text,
                              Map<String, Object> baseMetadata) {
        if (!notBlank(text)) return;
        out.add(new KnowledgeChunk(
                KnowledgeChunk.SOURCE_HEALTH_RECORD,
                r.getId(),
                r.getPatientId(),
                fieldKey,
                text.trim(),
                baseMetadata));
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
