package com.hospital.medication.dto;

import com.hospital.medication.domain.MedicationStatus;
import jakarta.validation.constraints.NotNull;

public record MedicationStatusChangeRequest(@NotNull MedicationStatus targetStatus) {
}
