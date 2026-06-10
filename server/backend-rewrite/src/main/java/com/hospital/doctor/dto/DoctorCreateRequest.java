package com.hospital.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6) String password,
    String uuidNumber, @NotBlank String fullName, String photoUrl,
    Integer sexTypes, String phone, String idNumber, String email,
    Long departmentId
) {}
