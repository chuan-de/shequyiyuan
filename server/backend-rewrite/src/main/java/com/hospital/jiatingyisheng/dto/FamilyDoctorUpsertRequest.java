package com.hospital.jiatingyisheng.dto;

import jakarta.validation.constraints.NotNull;

public record FamilyDoctorUpsertRequest(@NotNull Long residentId, @NotNull Long doctorId, String servicePackage, String validTo) {}
