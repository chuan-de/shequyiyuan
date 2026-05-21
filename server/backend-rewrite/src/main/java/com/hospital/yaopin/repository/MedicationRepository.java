package com.hospital.yaopin.repository;

import com.hospital.yaopin.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
}
