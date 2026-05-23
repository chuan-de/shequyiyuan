package com.hospital.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicalRecordUpsertRequest(
        @NotNull Long doctorId,
        @NotNull Long patientId,
        @NotBlank String caseNumber,
        @NotBlank String caseName,
        @NotBlank String conditionDesc,
        @NotBlank String examItems,
        @NotBlank String examResults,
        String doctorUuidNumber,
        String doctorName,
        String doctorPhone,
        String doctorIdNumber,
        String doctorEmail,
        String patientName,
        String patientPhone,
        String patientIdNumber,
        String patientEmail,
        Long version
) {}
