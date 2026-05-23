package com.hospital.reception.dto;

import jakarta.validation.constraints.NotBlank;

public record ReceptionUpdateRequest(
    @NotBlank String fullName,
    String uuidNumber,
    String phone,
    String email,
    Integer sexTypes
) {}
