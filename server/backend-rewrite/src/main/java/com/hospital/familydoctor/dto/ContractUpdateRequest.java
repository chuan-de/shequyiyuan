package com.hospital.familydoctor.dto;

import java.time.LocalDate;

public record ContractUpdateRequest(
    String servicePackage,
    LocalDate expiresAt,
    String notes
) {}
