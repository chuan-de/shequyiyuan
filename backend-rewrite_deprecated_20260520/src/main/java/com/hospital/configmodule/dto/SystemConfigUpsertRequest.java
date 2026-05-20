package com.hospital.configmodule.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemConfigUpsertRequest(@NotBlank String configKey, @NotBlank String configValue, Long version) {}
