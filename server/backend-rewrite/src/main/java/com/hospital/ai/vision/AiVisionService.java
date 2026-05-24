package com.hospital.ai.vision;

/**
 * Provider-agnostic seam for vision OCR. Phase 1 ships
 * {@link DoubaoVisionService}; future providers (self-hosted, alternative
 * regional clouds) plug in here without touching the controller or the
 * extractor.
 */
public interface AiVisionService {

    /**
     * Run vision OCR over the given image and return both the raw upstream
     * payload and the extractor-mapped structured fields. Persistence of an
     * {@link AiExtractionHistory} row happens inside the service so callers
     * always have an auditable trail.
     *
     * @throws com.hospital.ai.common.AiRateLimitException when the caller's
     *         AI quota is exhausted (mapped to HTTP 429 by
     *         {@link com.hospital.common.ApiExceptionHandler}).
     */
    AiVisionResult extractMedicalRecord(VisionExtractRequest request);
}
