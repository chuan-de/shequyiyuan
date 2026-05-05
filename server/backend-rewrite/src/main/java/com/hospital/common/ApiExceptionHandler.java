package com.hospital.common;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(ErrorCode.CONFLICT, ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error", List.of()));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + (fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage());
    }
}
