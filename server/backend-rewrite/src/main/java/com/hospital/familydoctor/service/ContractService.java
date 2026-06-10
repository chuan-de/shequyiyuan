package com.hospital.familydoctor.service;

import com.hospital.familydoctor.domain.ContractStatus;
import com.hospital.familydoctor.dto.ContractCreateRequest;
import com.hospital.familydoctor.dto.ContractResponse;
import com.hospital.familydoctor.dto.ContractUpdateRequest;
import java.util.List;

public interface ContractService {
    List<ContractResponse> list(Long patientId, Long familyDoctorId, ContractStatus status, String patientName);
    ContractResponse detail(Long id);
    ContractResponse create(ContractCreateRequest request, String actor);
    ContractResponse update(Long id, ContractUpdateRequest request, String actor);
    ContractResponse changeStatus(Long id, ContractStatus targetStatus, String actor);
}
