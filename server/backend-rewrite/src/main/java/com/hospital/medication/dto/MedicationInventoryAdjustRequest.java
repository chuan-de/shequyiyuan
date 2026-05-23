package com.hospital.medication.dto;

import jakarta.validation.constraints.NotNull;

public record MedicationInventoryAdjustRequest(@NotNull Integer delta, String reason) {}
