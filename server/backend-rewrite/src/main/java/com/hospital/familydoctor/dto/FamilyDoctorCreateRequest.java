package com.hospital.familydoctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FamilyDoctorCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String fullName,
    String photoUrl,
    Integer sexTypes,
    String phone,
    String email
) {}
