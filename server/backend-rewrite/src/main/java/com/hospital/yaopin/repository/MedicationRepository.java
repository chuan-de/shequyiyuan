package com.hospital.yaopin.repository;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import java.util.List;
import java.util.Optional;

public interface MedicationRepository {
    List<Medication> findAll(String keyword, MedicationStatus status);
    Optional<Medication> findById(Long id);
    Medication save(Medication medication);
}
