package com.hospital.common;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    boolean success,
    String errorCode,
    String message,
    List<String> details,
    String traceId,
    Instant timestamp
) {

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<String> details, String traceId) {
        return new ApiErrorResponse(false, errorCode.name(), message, details, traceId, Instant.now());
    }
}
