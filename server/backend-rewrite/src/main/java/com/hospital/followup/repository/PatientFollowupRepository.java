package com.hospital.followup.repository;

import com.hospital.followup.domain.PatientFollowup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientFollowupRepository extends JpaRepository<PatientFollowup, Long> {
}
