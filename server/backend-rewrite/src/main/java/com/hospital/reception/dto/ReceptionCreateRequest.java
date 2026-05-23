package com.hospital.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReceptionCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String fullName,
    String uuidNumber,
    String phone,
    String email,
    Integer sexTypes
) {}
