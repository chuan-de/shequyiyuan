package com.hospital.patient.dto;

import jakarta.validation.constraints.Size;

public record PatientResetPasswordRequest(
        @Size(min = 6, message = "Password must be at least 6 characters") String newPassword
) {}
