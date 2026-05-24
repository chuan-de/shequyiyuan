package com.hospital.ai.common;

/**
 * Thrown when an AI call would exceed the per-user QPM or daily token budget.
 *
 * <p>Mapped to HTTP 429 by {@link com.hospital.common.ApiExceptionHandler}.</p>
 */
public class AiRateLimitException extends RuntimeException {

    private final String reason;

    public AiRateLimitException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** Machine-readable reason: {@code "qpm"} or {@code "daily-token-budget"}. */
    public String getReason() { return reason; }
}
