package com.hospital.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String fullName,
    String photoUrl,
    String phone,
    String idNumber,
    String email,
    Integer sexTypes,
    LocalDate birthDate,
    String address,
    String allergies,
    String medicalHistory,
    String emergencyContactName,
    String emergencyContactPhone
) {}
