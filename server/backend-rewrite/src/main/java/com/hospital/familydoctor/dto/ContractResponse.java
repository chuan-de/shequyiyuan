package com.hospital.familydoctor.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ContractResponse(
    Long id,
    Long patientId,
    String patientName,
    String patientPhone,
    Long familyDoctorId,
    String familyDoctorName,
    String familyDoctorPhone,
    String servicePackage,
    LocalDate signedAt,
    LocalDate expiresAt,
    String status,
    String notes,
    Instant createdAt
) {}
