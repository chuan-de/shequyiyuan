package com.hospital.common;

import java.util.Map;

import com.hospital.ai.common.AiConsentRequiredException;
import com.hospital.ai.common.AiRateLimitException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Validation failed"));
    }

    @ExceptionHandler(AiRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleAiRateLimit(AiRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "message", ex.getMessage(),
                "reason", ex.getReason()
            ));
    }

    @ExceptionHandler(AiConsentRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleAiConsentRequired(AiConsentRequiredException ex) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
            .body(Map.of(
                "code", AiConsentRequiredException.ERROR_CODE,
                "message", ex.getMessage()
            ));
    }
}
