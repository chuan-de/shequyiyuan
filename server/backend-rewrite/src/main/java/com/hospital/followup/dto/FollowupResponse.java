package com.hospital.followup.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FollowupResponse(
    Long id,
    Long patientId,
    String patientName,
    Instant measuredAt,
    Integer systolic,
    Integer diastolic,
    BigDecimal bloodSugar,
    BigDecimal heightCm,
    BigDecimal weightKg,
    BigDecimal bmi,
    Integer heartRate,
    String notes,
    String recordedBy,
    Instant createdAt
) {}
