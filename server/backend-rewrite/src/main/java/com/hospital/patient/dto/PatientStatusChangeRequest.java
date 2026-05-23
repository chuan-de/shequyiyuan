package com.hospital.patient.dto;

import jakarta.validation.constraints.NotNull;

public record PatientStatusChangeRequest(@NotNull Boolean enabled) {}
