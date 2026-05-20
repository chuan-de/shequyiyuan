package com.hospital.jiuzhen.dto;

import jakarta.validation.constraints.NotBlank;

public record VisitUpsertRequest(
    @NotBlank String patientName,
    @NotBlank String doctorName,
    Long version
) {
}
