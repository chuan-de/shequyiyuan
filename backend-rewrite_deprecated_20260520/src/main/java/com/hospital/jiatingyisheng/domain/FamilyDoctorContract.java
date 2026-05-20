package com.hospital.jiatingyisheng.domain;

public record FamilyDoctorContract(Long id, Long residentId, Long doctorId, FamilyDoctorStatus status, Long version) {}
