package com.hospital.yisheng.domain;

public record DoctorProfile(Long id, String name, String department, DoctorStatus status, Long version) {
}
