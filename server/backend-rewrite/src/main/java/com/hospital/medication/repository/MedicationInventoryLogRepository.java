package com.hospital.medication.repository;

import com.hospital.medication.domain.MedicationInventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationInventoryLogRepository extends JpaRepository<MedicationInventoryLog, Long> {
}
