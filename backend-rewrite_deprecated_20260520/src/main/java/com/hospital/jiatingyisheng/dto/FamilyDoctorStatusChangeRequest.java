package com.hospital.jiatingyisheng.dto;

import com.hospital.jiatingyisheng.domain.FamilyDoctorStatus;
import jakarta.validation.constraints.NotNull;

public record FamilyDoctorStatusChangeRequest(@NotNull FamilyDoctorStatus targetStatus, String reason) {}
