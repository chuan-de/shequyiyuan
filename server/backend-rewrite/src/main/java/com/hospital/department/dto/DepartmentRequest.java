package com.hospital.department.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
    @NotBlank String deptName,
    String description, String headPerson, String phone, Integer deptTypes
) {}
