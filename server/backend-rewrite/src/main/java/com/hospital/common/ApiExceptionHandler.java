package com.hospital.common;

import java.util.Map;

import com.hospital.ai.common.AiConsentRequiredException;
import com.hospital.ai.common.AiRateLimitException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
        // 412 PRECONDITION_FAILED (not 428 PRECONDITION_REQUIRED): the frontend
        // explicitly checks for 412 in api.ts#askPatientAi to pop the consent
        // modal. PRECONDITION_FAILED is the closer semantic match — the
        // precondition (patient consent) was not satisfied.
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
            .body(Map.of(
                "code", AiConsentRequiredException.ERROR_CODE,
                "message", ex.getMessage()
            ));
    }

    /** 兜底：未预期异常统一回 500 JSON，不向客户端泄漏堆栈。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) throws Exception {
        // 安全异常必须继续抛给 Spring Security 的过滤器链，否则 401/403 会被吞成 500；
        // ResponseStatusException 等自带状态码的异常交还框架按其声明的状态码处理。
        if (ex instanceof org.springframework.security.access.AccessDeniedException
                || ex instanceof org.springframework.security.core.AuthenticationException
                || ex instanceof org.springframework.web.ErrorResponse) {
            throw ex;
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "服务器内部错误，请稍后重试"));
    }
}
