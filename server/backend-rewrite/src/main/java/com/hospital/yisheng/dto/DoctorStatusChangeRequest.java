package com.hospital.yisheng.dto;

import com.hospital.yisheng.domain.DoctorStatus;
import jakarta.validation.constraints.NotNull;

public record DoctorStatusChangeRequest(@NotNull DoctorStatus targetStatus) {
}
