package com.hospital.yaopin.repository;

import com.hospital.yaopin.domain.Medication;
import java.util.List;
import java.util.Optional;

public interface MedicationRepository {
    List<Medication> findAll(String keyword, Medication status);
    Optional<Medication> findById(Long id);
    Medication save(Medication medication);
}
