package com.hospital.department.repository;

import com.hospital.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDeptName(String deptName);
    boolean existsByDeptNameAndIdNot(String deptName, Long id);
}
