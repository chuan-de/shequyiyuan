package com.hospital.familydoctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FamilyDoctorResetPasswordRequest(@NotBlank @Size(min = 6) String newPassword) {}
