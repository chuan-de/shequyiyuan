package com.hospital.ai.vision;

import java.util.UUID;

/**
 * Service-layer input for a vision OCR extraction. Either {@code photoId} is
 * provided (server fetches the bytes from the {@code photo} table) or
 * {@code base64} + {@code contentType} are provided directly (legacy fallback
 * for clients that haven't uploaded yet).
 */
public record VisionExtractRequest(
        UUID photoId,
        String base64,
        String contentType,
        String prompt
) {
    public boolean hasPhotoId() { return photoId != null; }
    public boolean hasInlineData() { return base64 != null && !base64.isBlank() && contentType != null; }
}
