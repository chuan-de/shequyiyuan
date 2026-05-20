package com.hospital.yisheng.repository;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository {
    List<DoctorProfile> findAll(String keyword, DoctorStatus status);
    Optional<DoctorProfile> findById(Long id);
    DoctorProfile save(DoctorProfile doctor);
}
