package com.hospital.jiatingyisheng.repository;

import com.hospital.jiatingyisheng.domain.FamilyDoctorContract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyDoctorRepository extends JpaRepository<FamilyDoctorContract, Long> {
}
