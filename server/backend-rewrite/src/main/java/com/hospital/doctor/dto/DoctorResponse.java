package com.hospital.doctor.dto;

import java.time.Instant;

public record DoctorResponse(
    Long id, Long userId, String username, boolean enabled,
    String uuidNumber, String fullName, String photoUrl,
    Integer sexTypes, String phone, String idNumber, String email,
    Long departmentId, String departmentName,
    Instant createdAt
) {}
