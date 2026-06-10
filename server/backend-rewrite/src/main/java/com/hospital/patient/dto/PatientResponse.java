package com.hospital.patient.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
    Long id,
    Long userId,
    String username,
    boolean enabled,
    String fullName,
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
    String emergencyContactPhone,
    Instant createdAt
) {}
