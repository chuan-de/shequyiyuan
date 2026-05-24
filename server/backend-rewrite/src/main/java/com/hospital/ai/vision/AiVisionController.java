package com.hospital.ai.vision;

import com.hospital.common.ApiResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for vision OCR (Phase 1, Feature 1).
 *
 * <p>Disabled entirely when {@code hospital.ai.features.vision=false} — the
 * controller bean is never registered, so the route 404s and the frontend
 * "AI 识别" button (also feature-gated) just won't appear.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/vision")
@ConditionalOnProperty(
        prefix = "hospital.ai",
        name = {"enabled", "features.vision"},
        havingValue = "true")
public class AiVisionController {

    private final AiVisionService visionService;

    public AiVisionController(AiVisionService visionService) {
        this.visionService = visionService;
    }

    /**
     * Run OCR over an uploaded medical-record image. Auth via {@code ai:vision}
     * permission; rate limit + audit happen inside the service via the AOP
     * interceptor wrapped around {@code AiCallTemplate}.
     */
    @PostMapping("/parse-medical-record")
    @PreAuthorize("hasAuthority('ai:vision')")
    public ApiResponse<AiVisionResponse> parseMedicalRecord(@RequestBody VisionParseHttpRequest request) {
        if (request == null
                || (request.photoId() == null
                && (request.base64() == null || request.base64().isBlank()
                        || request.contentType() == null || request.contentType().isBlank()))) {
            // 400, not 409 — ApiExceptionHandler maps IllegalArgumentException
            // to CONFLICT for legacy business-rule reasons, so we throw the
            // explicit ResponseStatusException to opt out of that mapping.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "必须提供 photoId 或 base64 + contentType 之一");
        }
        VisionExtractRequest serviceRequest = new VisionExtractRequest(
                request.photoId(),
                request.base64(),
                request.contentType(),
                request.prompt()
        );
        AiVisionResult result = visionService.extractMedicalRecord(serviceRequest);
        return ApiResponse.ok(AiVisionResponse.from(result));
    }
}
