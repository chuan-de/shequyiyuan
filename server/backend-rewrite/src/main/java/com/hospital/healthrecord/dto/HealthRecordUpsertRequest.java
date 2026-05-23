package com.hospital.healthrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record HealthRecordUpsertRequest(
        @NotNull Long patientId,
        @NotBlank String title,
        String otherMembers,
        Integer unitTypes,
        Instant recordedAt,
        String content,
        String patientName,
        String patientPhone,
        String patientIdNumber,
        String patientEmail,
        Long version
) {}
