package com.hospital.yisheng.dto;

import jakarta.validation.constraints.NotBlank;

public record DoctorUpsertRequest(
    @NotBlank String name,
    @NotBlank String department,
    Long version
) {
}
