package com.hospital.jiuankangdangan.domain;

import java.time.LocalDateTime;

public record JiuankangdanganRecord(
        Long id,
        Long yonghuId,
        String yonghuName,
        String yonghuPhone,
        String yonghuIdNumber,
        String yonghuEmail,
        String jiuankangdanganName,
        String jiuankangdanganQita,
        Integer jiuankangdanganTypes,
        LocalDateTime insertTime,
        String jiuankangdanganContent,
        JiuankangdanganStatus status,
        Long version
) {}
