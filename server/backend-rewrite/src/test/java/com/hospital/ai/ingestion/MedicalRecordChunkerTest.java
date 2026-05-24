package com.hospital.ai.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.domain.MedicalRecordStatus;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link MedicalRecordChunker} — no Spring context, no
 * embedding calls. Confirms the chunker:
 * <ul>
 *   <li>Emits per-field chunks for non-blank inputs.</li>
 *   <li>Skips blank / null fields silently (no empty chunks reach embedding).</li>
 *   <li>Renders structured prescription items to a single human-readable line.</li>
 *   <li>Carries patient_id + provenance metadata on every chunk.</li>
 * </ul>
 */
class MedicalRecordChunkerTest {

    private final MedicalRecordChunker chunker = new MedicalRecordChunker();

    @Test
    void chunk_populates_all_meaningful_fields() {
        MedicalRecord r = new MedicalRecord(
                1L, 10L, "DOC10", "李医生", "13800000001", "11010119800101001X", "li@x.cn",
                42L, "王患者", "13900000002", "33010119900202002Y", "wang@y.cn",
                "BL2025010100100", "感冒就诊",
                "持续咳嗽 3 天", "血常规", "上呼吸道感染",
                List.of(Map.of("name", "阿莫西林", "quantity", 6, "unit", "粒"),
                        Map.of("name", "板蓝根冲剂", "quantity", 3, "unit", "袋")),
                List.of(), Instant.parse("2026-01-15T08:00:00Z"),
                MedicalRecordStatus.ACTIVE, 0L);

        List<KnowledgeChunk> chunks = chunker.chunk(r);

        // chief_complaint, diagnosis, prescription, exam — ai_extracted is null so skipped.
        assertEquals(4, chunks.size());
        chunks.forEach(c -> {
            assertEquals(42L, c.patientId());
            assertEquals(1L, c.sourceId());
            assertEquals(KnowledgeChunk.SOURCE_MEDICAL_RECORD, c.sourceType());
            assertNotNull(c.metadata().get("source_created_at"));
            assertEquals(10L, c.metadata().get("doctor_id"));
        });

        KnowledgeChunk prescription = chunks.stream()
                .filter(c -> "prescription".equals(c.fieldKey()))
                .findFirst().orElseThrow();
        assertTrue(prescription.chunkText().contains("阿莫西林"));
        assertTrue(prescription.chunkText().contains("× 6"));
        assertTrue(prescription.chunkText().contains("板蓝根冲剂"));
    }

    @Test
    void chunk_skips_blank_fields() {
        MedicalRecord r = new MedicalRecord(
                2L, null, null, null, null, null, null,
                7L, null, null, null, null,
                "BL2", "诊断A", "  ", null, null,
                null, null, null,
                MedicalRecordStatus.DRAFT, 0L);
        List<KnowledgeChunk> chunks = chunker.chunk(r);

        // Only diagnosis (from caseName when examResults is blank) makes it through.
        assertEquals(1, chunks.size());
        assertEquals("diagnosis", chunks.get(0).fieldKey());
        assertEquals("诊断A", chunks.get(0).chunkText());
    }

    @Test
    void chunk_includes_ai_extracted_when_present() {
        MedicalRecord r = new MedicalRecord(
                3L, null, null, null, null, null, null,
                7L, null, null, null, null,
                "BL3", null, null, null, null,
                null, null, null,
                MedicalRecordStatus.DRAFT, 0L);
        r.setAiExtracted(Map.of(
                "chiefComplaint", "胸闷",
                "diagnosis", "心绞痛"));

        List<KnowledgeChunk> chunks = chunker.chunk(r);
        KnowledgeChunk aiChunk = chunks.stream()
                .filter(c -> "ai_extracted".equals(c.fieldKey()))
                .findFirst().orElseThrow();
        assertTrue(aiChunk.chunkText().contains("胸闷"));
        assertTrue(aiChunk.chunkText().contains("心绞痛"));
    }

    @Test
    void chunk_returns_empty_when_patient_id_missing() {
        MedicalRecord r = new MedicalRecord(
                4L, null, null, null, null, null, null,
                null, null, null, null, null,
                "BL4", "X", "Y", null, null,
                null, null, null,
                MedicalRecordStatus.DRAFT, 0L);
        assertEquals(List.of(), chunker.chunk(r));
    }
}
