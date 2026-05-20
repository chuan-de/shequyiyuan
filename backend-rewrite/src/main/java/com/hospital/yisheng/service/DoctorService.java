package com.hospital.yisheng.service;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import com.hospital.yisheng.dto.DoctorUpsertRequest;
import java.util.List;

public interface DoctorService {
    List<DoctorProfile> list(String keyword, DoctorStatus status);
    DoctorProfile detail(Long id);
    DoctorProfile create(DoctorUpsertRequest request, String actor);
    DoctorProfile update(Long id, DoctorUpsertRequest request, String actor);
    DoctorProfile changeStatus(Long id, DoctorStatus status, String actor);
}
