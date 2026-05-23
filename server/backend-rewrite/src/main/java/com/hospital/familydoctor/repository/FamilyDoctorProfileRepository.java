package com.hospital.familydoctor.repository;

import com.hospital.familydoctor.domain.FamilyDoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyDoctorProfileRepository extends JpaRepository<FamilyDoctorProfile, Long> {
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
}
