package com.hospital.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientUpdateRequest(
    @NotBlank String fullName,
    String phone,
    String idNumber,
    String email,
    Integer sexTypes
) {}
