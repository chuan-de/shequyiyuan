package com.hospital.medication.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record MedicationUpsertRequest(
        @NotBlank String code,
        @NotBlank String name,
        Long version,
        BigDecimal price,
        Integer stock,
        String mainEffect,
        String sideEffect,
        String detail
) {
}
