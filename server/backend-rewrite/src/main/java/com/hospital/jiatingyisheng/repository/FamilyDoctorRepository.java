package com.hospital.jiatingyisheng.repository;

import com.hospital.jiatingyisheng.domain.FamilyDoctorContract;
import com.hospital.jiatingyisheng.domain.FamilyDoctorStatus;
import java.util.List;
import java.util.Optional;

public interface FamilyDoctorRepository {
    List<FamilyDoctorContract> findAll(String keyword, FamilyDoctorStatus status);
    Optional<FamilyDoctorContract> findById(Long id);
    FamilyDoctorContract save(FamilyDoctorContract contract);
}
