package com.hospital.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MedicalRecordUpsertRequest(
        @NotNull Long doctorId,
        @NotNull Long patientId,
        String caseNumber,
        @NotBlank String caseName,
        String conditionDesc,
        String examItems,
        String examResults,
        String doctorUuidNumber,
        String doctorName,
        String doctorPhone,
        String doctorIdNumber,
        String doctorEmail,
        String patientName,
        String patientPhone,
        String patientIdNumber,
        String patientEmail,
        List<Map<String, Object>> prescriptionItems,
        List<Map<String, Object>> attachments,
        Instant recordDate,
        Long visitId,
        Long version
) {}
