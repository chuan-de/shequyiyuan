package com.hospital.ai.rag;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.hospital.ai.audit.CurrentUserResolver;
import com.hospital.common.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the patient private-RAG feature (Phase 2.9).
 *
 * <p>Two endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/v1/ai/patient/{patientId}/ask} — answer a question
 *       grounded in THIS patient's records. 412 + {@code AI_CONSENT_REQUIRED}
 *       when {@code patient_profile.ai_consent_at} is null.</li>
 *   <li>{@code POST /api/v1/ai/patient/{patientId}/consent} — patient (or
 *       admin) grants the AI processing consent. Row-level check enforces
 *       that only the patient themselves or an admin can call it.</li>
 * </ul>
 *
 * <p>Whole controller is gated by {@code hospital.ai.features.patient-rag} —
 * when off the bean is not registered, so the route 404s instead of 403.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/patient")
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class PatientRagController {

    private final PatientRagService ragService;
    private final CurrentUserResolver currentUserResolver;

    public PatientRagController(PatientRagService ragService,
                                CurrentUserResolver currentUserResolver) {
        this.ragService = ragService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/{patientId}/ask")
    @PreAuthorize("hasAuthority('ai:patient-rag')")
    public ApiResponse<RagAnswerResponse> ask(@PathVariable Long patientId,
                                              @Valid @RequestBody AskRequest body) {
        Long callerId = currentUserResolver.currentUserId();
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户禁止访问");
        }
        RagAnswer answer = ragService.ask(patientId, body.question(), callerId);
        return ApiResponse.ok(RagAnswerResponse.from(answer));
    }

    @PostMapping("/{patientId}/consent")
    @PreAuthorize("hasAuthority('ai:patient-rag')")
    public ApiResponse<Map<String, Object>> grantConsent(@PathVariable Long patientId) {
        Long callerId = currentUserResolver.currentUserId();
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户禁止访问");
        }
        Instant consentedAt = ragService.grantConsent(patientId, callerId);
        return ApiResponse.ok(Map.of("consentedAt", consentedAt.toString()));
    }

    public record AskRequest(
            @NotBlank(message = "question 不能为空")
            @Size(max = 1000, message = "question 长度不能超过 1000 个字符")
            String question) {}

    public record RagAnswerResponse(
            String answer,
            List<Citation> citations,
            int tokensIn,
            int tokensOut,
            long latencyMs) {

        public static RagAnswerResponse from(RagAnswer a) {
            return new RagAnswerResponse(a.answer(), a.citations(),
                    a.tokensIn(), a.tokensOut(), a.latencyMs());
        }
    }
}
