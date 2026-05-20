package com.hospital.jiuzhen.domain;

public record VisitRecord(Long id, String patientName, String doctorName, VisitStatus status, Long version) {
}
