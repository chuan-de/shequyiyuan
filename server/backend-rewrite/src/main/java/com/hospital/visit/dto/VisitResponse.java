package com.hospital.visit.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VisitResponse(
    Long id, String visitNumber, BigDecimal fee, Integer keshiTypes,
    Instant visitDate, String registrationNotes, String visitContent,
    Long patientId, String patientFullName, String patientPhotoUrl,
    String patientPhone, String patientIdNumber, String patientEmail,
    Instant createdAt
) {}
