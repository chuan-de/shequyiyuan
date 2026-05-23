package com.hospital.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record VisitCreateRequest(
    @NotNull Long patientId,
    @NotBlank String visitNumber,
    BigDecimal fee,
    Integer keshiTypes,
    Instant visitDate,
    String registrationNotes,
    String visitContent
) {}
