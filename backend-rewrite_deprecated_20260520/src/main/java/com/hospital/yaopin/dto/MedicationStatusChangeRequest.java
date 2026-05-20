package com.hospital.yaopin.dto;

import com.hospital.yaopin.domain.MedicationStatus;
import jakarta.validation.constraints.NotNull;

public record MedicationStatusChangeRequest(@NotNull MedicationStatus targetStatus) {
}
