package com.hospital.medicalrecord.service;

import com.hospital.common.PageResponse;
import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.domain.MedicalRecordStatus;
import com.hospital.medicalrecord.dto.MedicalRecordUpsertRequest;

public interface MedicalRecordService {
    PageResponse<MedicalRecord> list(String keyword, MedicalRecordStatus status, Long patientId, int page, int size);
    MedicalRecord detail(Long id);
    MedicalRecord create(MedicalRecordUpsertRequest request, String actor);
    MedicalRecord update(Long id, MedicalRecordUpsertRequest request, String actor);
    MedicalRecord changeStatus(Long id, MedicalRecordStatus status, String actor);
    void delete(Long id, String actor);
}
