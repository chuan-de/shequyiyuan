package com.hospital.doctor.repository;

import com.hospital.doctor.domain.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    boolean existsByPhone(String phone);
    boolean existsByIdNumber(String idNumber);
    boolean existsByUuidNumber(String uuidNumber);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    boolean existsByIdNumberAndIdNot(String idNumber, Long id);
    boolean existsByUuidNumberAndIdNot(String uuidNumber, Long id);
}
