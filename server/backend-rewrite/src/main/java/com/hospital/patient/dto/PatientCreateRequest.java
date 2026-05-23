package com.hospital.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String fullName,
    String phone,
    String idNumber,
    String email,
    Integer sexTypes
) {}
