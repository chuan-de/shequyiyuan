package com.hospital.reception.dto;

import jakarta.validation.constraints.Size;

public record ReceptionResetPasswordRequest(
        @Size(min = 6, message = "Password must be at least 6 characters") String newPassword
) {}
