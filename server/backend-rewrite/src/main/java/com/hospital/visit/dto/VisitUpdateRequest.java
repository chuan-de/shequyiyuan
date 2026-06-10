package com.hospital.visit.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VisitUpdateRequest(
    BigDecimal fee,
    Integer keshiTypes,
    Long doctorId,
    Instant visitDate,
    String registrationNotes,
    String visitContent
) {}
