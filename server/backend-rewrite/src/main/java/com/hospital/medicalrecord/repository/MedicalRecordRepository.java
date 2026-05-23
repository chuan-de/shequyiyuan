package com.hospital.medicalrecord.repository;

import com.hospital.medicalrecord.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
}
