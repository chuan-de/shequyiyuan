package com.hospital.patient.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PatientUpdateRequest(
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
