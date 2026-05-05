package com.hospital.common;

import java.util.List;

public record ApiErrorResponse(
    boolean success,
    String errorCode,
    String message,
    List<String> details
) {

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<String> details) {
        return new ApiErrorResponse(false, errorCode.name(), message, details);
    }
}
