package com.hospital.systemconfig.dto;

import com.hospital.systemconfig.domain.ConfigStatus;
import jakarta.validation.constraints.NotNull;

public record SystemConfigStatusChangeRequest(@NotNull ConfigStatus targetStatus) {}
