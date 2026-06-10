package com.hospital.followup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record FollowupUpsertRequest(
    @NotNull Long patientId,
    Instant measuredAt,
    @Min(40) @Max(300) Integer systolic,
    @Min(20) @Max(200) Integer diastolic,
    BigDecimal bloodSugar,
    BigDecimal heightCm,
    BigDecimal weightKg,
    @Min(20) @Max(250) Integer heartRate,
    String notes
) {}
