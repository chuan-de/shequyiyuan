package com.hospital.ai.vision;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Maps the raw model JSON response into a {@link MedicalRecordFields} DTO.
 *
 * <p>Defensive on every front: the model is asked to return strict JSON but
 * sometimes wraps it in ``` fences or prose. We strip the wrapper, parse
 * leniently, and coerce types (e.g. age as "32" or "32 岁" → 32). Any field
 * missing or unparseable becomes {@code null} — the UI will simply not
 * pre-fill it.
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class MedicalRecordExtractor {

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*(.*?)```", Pattern.DOTALL);
    private static final Pattern FIRST_JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    private final ObjectMapper objectMapper;

    public MedicalRecordExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse the model's textual response into structured fields.
     *
     * @param modelResponse the raw {@code content} from the chat completion
     * @return parsed payload + the JSON map the parser was able to recover
     *         (useful for persisting to {@code ai_extraction_history.raw_json})
     */
    public Parsed parse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            return new Parsed(emptyMap(), emptyFields());
        }
        String json = stripMarkdownFence(modelResponse).trim();
        Map<String, Object> map;
        try {
            map = objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            Matcher m = FIRST_JSON_OBJECT.matcher(json);
            if (m.find()) {
                try {
                    map = objectMapper.readValue(m.group(), Map.class);
                } catch (JsonProcessingException ignored) {
                    return new Parsed(Map.of("error", "unparseable", "raw", truncate(json, 500)), emptyFields());
                }
            } else {
                return new Parsed(Map.of("error", "unparseable", "raw", truncate(json, 500)), emptyFields());
            }
        }
        MedicalRecordFields fields = new MedicalRecordFields(
                asString(map.get("patientName")),
                asString(map.get("gender")),
                asInteger(map.get("age")),
                asString(map.get("visitDate")),
                asString(map.get("department")),
                asString(map.get("chiefComplaint")),
                asString(map.get("presentIllness")),
                asString(map.get("diagnosis")),
                asString(map.get("prescription")),
                asString(map.get("doctor"))
        );
        return new Parsed(map, fields);
    }

    /** 0–100, two-decimal precision. Non-null fields ÷ total fields × 100. */
    public BigDecimal confidence(MedicalRecordFields f) {
        int filled = 0;
        if (notBlank(f.patientName())) filled++;
        if (notBlank(f.gender())) filled++;
        if (f.age() != null) filled++;
        if (notBlank(f.visitDate())) filled++;
        if (notBlank(f.department())) filled++;
        if (notBlank(f.chiefComplaint())) filled++;
        if (notBlank(f.presentIllness())) filled++;
        if (notBlank(f.diagnosis())) filled++;
        if (notBlank(f.prescription())) filled++;
        if (notBlank(f.doctor())) filled++;
        return BigDecimal.valueOf(filled)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(MedicalRecordFields.totalFieldCount()), 2, RoundingMode.HALF_UP);
    }

    public String status(MedicalRecordFields f, BigDecimal confidence) {
        if (confidence.compareTo(BigDecimal.ZERO) == 0) return AiExtractionHistory.STATUS_FAILED;
        if (confidence.compareTo(BigDecimal.valueOf(50)) < 0) return AiExtractionHistory.STATUS_PARTIAL;
        return AiExtractionHistory.STATUS_SUCCESS;
    }

    // --- internals ---------------------------------------------------------

    static String stripMarkdownFence(String s) {
        Matcher m = JSON_FENCE.matcher(s);
        if (m.find()) return m.group(1);
        return s;
    }

    static String asString(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s.isBlank() ? null : s.trim();
        return o.toString();
    }

    static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) return null;
            // Strip common Chinese / English suffixes: "32 岁", "32 years"
            Matcher m = Pattern.compile("-?\\d+").matcher(trimmed);
            if (m.find()) {
                try { return Integer.parseInt(m.group()); }
                catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max);
    }

    private static MedicalRecordFields emptyFields() {
        return new MedicalRecordFields(null, null, null, null, null, null, null, null, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> emptyMap() {
        return (Map) java.util.Collections.emptyMap();
    }

    /** Combined parser output: original map (for {@code raw_json}) + DTO. */
    public record Parsed(Map<String, Object> rawJson, MedicalRecordFields fields) { }
}
