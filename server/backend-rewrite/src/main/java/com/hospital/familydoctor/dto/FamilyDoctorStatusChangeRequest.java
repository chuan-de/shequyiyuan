package com.hospital.familydoctor.dto;

import jakarta.validation.constraints.NotNull;

public record FamilyDoctorStatusChangeRequest(@NotNull Boolean enabled) {}
