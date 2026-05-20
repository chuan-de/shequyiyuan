package com.hospital.bingli.dto;

import com.hospital.bingli.domain.BingliStatus;
import jakarta.validation.constraints.NotNull;

public record BingliStatusChangeRequest(@NotNull BingliStatus targetStatus) {}
