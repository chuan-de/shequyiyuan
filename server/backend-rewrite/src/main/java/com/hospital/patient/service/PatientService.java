package com.hospital.patient.service;

import com.hospital.patient.dto.PatientCreateRequest;
import com.hospital.patient.dto.PatientResponse;
import com.hospital.patient.dto.PatientUpdateRequest;
import java.util.List;

public interface PatientService {
    List<PatientResponse> list(String keyword);
    PatientResponse detail(Long id);
    PatientResponse create(PatientCreateRequest request, String actor);
    PatientResponse update(Long id, PatientUpdateRequest request, String actor);
    PatientResponse changeStatus(Long id, boolean enabled, String actor);
}
