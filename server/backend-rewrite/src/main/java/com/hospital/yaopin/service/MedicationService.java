package com.hospital.yaopin.service;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import com.hospital.yaopin.dto.MedicationUpsertRequest;
import java.util.List;

public interface MedicationService {
    List<Medication> list(String keyword, MedicationStatus status);
    Medication detail(Long id);
    Medication create(MedicationUpsertRequest request, String actor);
    Medication update(Long id, MedicationUpsertRequest request, String actor);
    Medication changeStatus(Long id, MedicationStatus status, String actor);
}
