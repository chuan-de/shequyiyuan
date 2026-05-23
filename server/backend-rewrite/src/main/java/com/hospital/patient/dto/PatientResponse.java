package com.hospital.patient.dto;

import java.time.Instant;

public record PatientResponse(
    Long id,
    Long userId,
    String username,
    boolean enabled,
    String fullName,
    String phone,
    String idNumber,
    String email,
    Integer sexTypes,
    Instant createdAt
) {}
