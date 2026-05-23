package com.hospital.systemconfig.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemConfigUpsertRequest(@NotBlank String configKey, @NotBlank String configValue, Long version) {}
