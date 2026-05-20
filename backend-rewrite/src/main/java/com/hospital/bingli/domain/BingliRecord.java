package com.hospital.bingli.domain;

public record BingliRecord(
        Long id,
        Long yishengId,
        String yishengUuidNumber,
        String yishengName,
        String yishengPhone,
        String yishengIdNumber,
        String yishengEmail,
        Long yonghuId,
        String yonghuName,
        String yonghuPhone,
        String yonghuIdNumber,
        String yonghuEmail,
        String bingliUuidNumber,
        String bingliName,
        String bingliBingqing,
        String jianchaxiangmu,
        String jianchajieguo,
        BingliStatus status,
        Long version
) {}
