package com.hospital.configmodule.dto;

import com.hospital.configmodule.domain.ConfigStatus;
import jakarta.validation.constraints.NotNull;

public record SystemConfigStatusChangeRequest(@NotNull ConfigStatus targetStatus) {}
