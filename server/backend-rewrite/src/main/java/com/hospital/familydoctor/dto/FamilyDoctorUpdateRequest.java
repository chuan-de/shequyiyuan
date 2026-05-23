package com.hospital.familydoctor.dto;

import jakarta.validation.constraints.NotBlank;

public record FamilyDoctorUpdateRequest(
    @NotBlank String fullName,
    String photoUrl,
    Integer sexTypes,
    String phone,
    String email
) {}
