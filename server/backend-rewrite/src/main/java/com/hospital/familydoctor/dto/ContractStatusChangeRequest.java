package com.hospital.familydoctor.dto;

import com.hospital.familydoctor.domain.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusChangeRequest(@NotNull ContractStatus targetStatus) {}
