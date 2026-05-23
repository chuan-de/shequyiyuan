package com.hospital.department.dto;

import java.time.Instant;

public record DepartmentResponse(
    Long id, String deptName, String description,
    String headPerson, String phone, Integer deptTypes,
    Instant createdAt, Instant updatedAt
) {}
