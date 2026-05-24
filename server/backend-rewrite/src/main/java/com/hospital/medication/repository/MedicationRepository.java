package com.hospital.medication.repository;

import com.hospital.medication.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    boolean existsByCode(String code);
}
