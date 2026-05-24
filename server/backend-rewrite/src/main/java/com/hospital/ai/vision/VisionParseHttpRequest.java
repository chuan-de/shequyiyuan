package com.hospital.ai.vision;

import java.util.UUID;

/**
 * HTTP body for {@code POST /api/v1/ai/vision/parse-medical-record}. Either
 * {@code photoId} or {@code base64 + contentType} must be supplied; the
 * controller validates the combination and returns 400 if neither is
 * present.
 */
public record VisionParseHttpRequest(
        UUID photoId,
        String base64,
        String contentType,
        String prompt
) { }
