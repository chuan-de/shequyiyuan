package com.hospital.jiuankangdangan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record JiuankangdanganUpsertRequest(
        @NotNull Long yonghuId,
        @NotBlank String jiuankangdanganName,
        @NotBlank String jiuankangdanganQita,
        @NotNull Integer jiuankangdanganTypes,
        @NotNull LocalDateTime insertTime,
        @NotBlank String jiuankangdanganContent,
        String yonghuName,
        String yonghuPhone,
        String yonghuIdNumber,
        String yonghuEmail,
        Long version
) {}
