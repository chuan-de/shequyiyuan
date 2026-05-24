package com.hospital.ai.vision;

import java.util.Map;

/**
 * Full result of a vision OCR extraction — surfaced both to controllers (for
 * the JSON response) and to history persistence (for the {@code raw_json}
 * column).
 */
public record AiVisionResult(
        Map<String, Object> rawJson,
        MedicalRecordFields fields,
        double confidence,
        int tokensIn,
        int tokensOut,
        long latencyMs,
        Long extractionHistoryId
) {
    /** Returns a copy with the freshly-persisted history id attached. */
    public AiVisionResult withHistoryId(Long id) {
        return new AiVisionResult(rawJson, fields, confidence, tokensIn, tokensOut, latencyMs, id);
    }
}
