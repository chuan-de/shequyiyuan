package com.hospital.yaopin.domain;

public record Medication(Long id, String code, String name, MedicationStatus status, Long version) {
}
