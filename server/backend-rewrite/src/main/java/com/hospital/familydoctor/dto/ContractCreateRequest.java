package com.hospital.familydoctor.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ContractCreateRequest(
    @NotNull Long patientId,
    @NotNull Long familyDoctorId,
    String servicePackage,
    LocalDate signedAt,
    LocalDate expiresAt,
    String notes
) {}
