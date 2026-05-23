package com.hospital.medicalrecord.dto;

import com.hospital.medicalrecord.domain.MedicalRecordStatus;
import jakarta.validation.constraints.NotNull;

public record MedicalRecordStatusChangeRequest(@NotNull MedicalRecordStatus targetStatus) {}
