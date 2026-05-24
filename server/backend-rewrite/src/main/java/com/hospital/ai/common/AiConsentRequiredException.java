package com.hospital.ai.common;

/**
 * Thrown by {@code PatientRagService} when the patient has not yet granted
 * AI processing consent ({@code patient_profile.ai_consent_at IS NULL}).
 *
 * <p>Mapped to HTTP 412 Precondition Required + machine-readable error code
 * {@code AI_CONSENT_REQUIRED} so the UI can pop the consent modal without
 * having to inspect the message.</p>
 */
public class AiConsentRequiredException extends RuntimeException {

    public static final String ERROR_CODE = "AI_CONSENT_REQUIRED";

    public AiConsentRequiredException(String message) {
        super(message);
    }
}
