package com.hospital.healthrecord.dto;

import com.hospital.healthrecord.domain.HealthRecordStatus;
import jakarta.validation.constraints.NotNull;

public record HealthRecordStatusChangeRequest(@NotNull HealthRecordStatus targetStatus) {}
