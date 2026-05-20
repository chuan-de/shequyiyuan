package com.hospital.yaopin.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicationUpsertRequest(
        @NotBlank String code,
        @NotBlank String name,
        Long version
) {
}
