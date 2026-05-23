package com.hospital.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorResetPasswordRequest(@NotBlank @Size(min = 6) String newPassword) {}
