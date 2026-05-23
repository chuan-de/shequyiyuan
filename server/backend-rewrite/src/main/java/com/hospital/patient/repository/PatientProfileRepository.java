package com.hospital.patient.repository;

import com.hospital.patient.domain.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    boolean existsByPhone(String phone);
    boolean existsByIdNumber(String idNumber);

    @Query("SELECT COUNT(p) > 0 FROM PatientProfile p WHERE p.phone = :phone AND p.id <> :id")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("id") Long id);

    @Query("SELECT COUNT(p) > 0 FROM PatientProfile p WHERE p.idNumber = :idNumber AND p.id <> :id")
    boolean existsByIdNumberAndIdNot(@Param("idNumber") String idNumber, @Param("id") Long id);
}
