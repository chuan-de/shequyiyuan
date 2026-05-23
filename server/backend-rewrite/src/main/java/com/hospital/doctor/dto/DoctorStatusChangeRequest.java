package com.hospital.doctor.dto;

import jakarta.validation.constraints.NotNull;

public record DoctorStatusChangeRequest(@NotNull Boolean enabled) {}
