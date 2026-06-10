package com.hospital.familydoctor.repository;

import com.hospital.familydoctor.domain.ContractStatus;
import com.hospital.familydoctor.domain.FamilyDoctorContract;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyDoctorContractRepository extends JpaRepository<FamilyDoctorContract, Long> {
    boolean existsByPatientIdAndStatus(Long patientId, ContractStatus status);
    List<FamilyDoctorContract> findByPatientIdOrderBySignedAtDesc(Long patientId);
}
