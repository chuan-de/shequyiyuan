package com.hospital.reception.dto;

import java.time.Instant;

public record ReceptionResponse(
    Long id,
    Long userId,
    String username,
    boolean enabled,
    String uuidNumber,
    String fullName,
    String phone,
    String email,
    Integer sexTypes,
    Instant createdAt
) {}
