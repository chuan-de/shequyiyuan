package com.hospital.visit.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;

public record VisitUpdateRequest(
    @NotBlank String visitNumber,
    BigDecimal fee,
    Integer keshiTypes,
    Instant visitDate,
    String registrationNotes,
    String visitContent
) {}
