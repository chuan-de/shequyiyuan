package com.hospital.bingli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BingliUpsertRequest(
        @NotNull Long yishengId,
        @NotNull Long yonghuId,
        @NotBlank String bingliUuidNumber,
        @NotBlank String bingliName,
        @NotBlank String bingliBingqing,
        @NotBlank String jianchaxiangmu,
        @NotBlank String jianchajieguo,
        String yishengUuidNumber,
        String yishengName,
        String yishengPhone,
        String yishengIdNumber,
        String yishengEmail,
        String yonghuName,
        String yonghuPhone,
        String yonghuIdNumber,
        String yonghuEmail,
        Long version
) {}
