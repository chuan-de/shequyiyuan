package com.hospital.department.service;

import com.hospital.department.dto.DepartmentRequest;
import com.hospital.department.dto.DepartmentResponse;
import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> list(String deptName, Integer deptTypes);
    DepartmentResponse detail(Long id);
    DepartmentResponse create(DepartmentRequest request, String actor);
    DepartmentResponse update(Long id, DepartmentRequest request, String actor);
    void delete(Long id, String actor);
}
