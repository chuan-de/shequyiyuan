package com.hospital.familydoctor.dto;

import java.time.Instant;

public record FamilyDoctorResponse(
    Long id, Long userId, String username, boolean enabled,
    String fullName, String photoUrl, Integer sexTypes, String phone, String email,
    Instant createdAt
) {}
