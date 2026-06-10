package com.hospital.medicalrecord.repository;

import com.hospital.medicalrecord.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long>, JpaSpecificationExecutor<MedicalRecord> {
    boolean existsByCaseNumber(String caseNumber);
}
