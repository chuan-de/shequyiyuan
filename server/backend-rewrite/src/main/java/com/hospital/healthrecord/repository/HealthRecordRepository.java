package com.hospital.healthrecord.repository;

import com.hospital.healthrecord.domain.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
}
