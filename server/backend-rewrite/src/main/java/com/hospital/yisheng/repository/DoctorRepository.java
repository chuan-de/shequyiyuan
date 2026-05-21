package com.hospital.yisheng.repository;

import com.hospital.yisheng.domain.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<DoctorProfile, Long> {
}
