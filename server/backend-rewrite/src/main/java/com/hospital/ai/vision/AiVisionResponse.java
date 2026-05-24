package com.hospital.ai.vision;

import java.util.Map;

/**
 * JSON body returned by {@link AiVisionController#parseMedicalRecord(VisionParseHttpRequest)}.
 * Frontend uses {@code fields} to pre-populate the suggestion panel and shows
 * {@code tokensIn / tokensOut / latencyMs} so the medic can see the cost of
 * the call.
 */
public record AiVisionResponse(
        Map<String, Object> rawJson,
        MedicalRecordFields fields,
        double confidence,
        int tokensIn,
        int tokensOut,
        long latencyMs,
        Long extractionHistoryId
) {
    public static AiVisionResponse from(AiVisionResult result) {
        return new AiVisionResponse(
                result.rawJson(),
                result.fields(),
                result.confidence(),
                result.tokensIn(),
                result.tokensOut(),
                result.latencyMs(),
                result.extractionHistoryId()
        );
    }
}
