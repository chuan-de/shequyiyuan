package com.hospital.doctor.dto;

import jakarta.validation.constraints.NotBlank;

public record DoctorUpdateRequest(
    String uuidNumber, @NotBlank String fullName, String photoUrl,
    Integer sexTypes, String phone, String idNumber, String email,
    Long departmentId
) {}
