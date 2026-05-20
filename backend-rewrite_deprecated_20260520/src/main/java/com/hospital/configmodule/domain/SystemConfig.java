package com.hospital.configmodule.domain;

public record SystemConfig(Long id, String configKey, String configValue, ConfigStatus status, Long version) {}
