package com.hospital.medication.service;

import com.hospital.medication.domain.Medication;
import com.hospital.medication.domain.MedicationStatus;
import com.hospital.medication.dto.MedicationUpsertRequest;
import java.util.List;

public interface MedicationService {
    List<Medication> list(String keyword, MedicationStatus status);
    Medication detail(Long id);
    Medication create(MedicationUpsertRequest request, String actor);
    Medication update(Long id, MedicationUpsertRequest request, String actor);
    Medication changeStatus(Long id, MedicationStatus status, String actor);
    void adjustInventory(Long id, int delta, String reason, String operator);
}
