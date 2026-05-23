package com.hospital.department.service;

import com.hospital.common.NotFoundException;
import com.hospital.department.domain.Department;
import com.hospital.department.dto.DepartmentRequest;
import com.hospital.department.dto.DepartmentResponse;
import com.hospital.department.repository.DepartmentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultDepartmentService implements DepartmentService {

    private static final String SELECT_SQL = """
        SELECT id, dept_name, description, head_person, phone, dept_types, created_at, updated_at
        FROM department
        """;

    private final DepartmentRepository departmentRepo;
    private final JdbcClient jdbcClient;

    public DefaultDepartmentService(DepartmentRepository departmentRepo, JdbcClient jdbcClient) {
        this.departmentRepo = departmentRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(String deptName, Integer deptTypes) {
        String sql = SELECT_SQL + """
            WHERE (CAST(:deptName AS TEXT) IS NULL OR dept_name ILIKE '%' || :deptName || '%')
              AND (CAST(:deptTypes AS INTEGER) IS NULL OR dept_types = :deptTypes)
            ORDER BY created_at DESC
            """;
        return jdbcClient.sql(sql)
            .param("deptName", blank(deptName))
            .param("deptTypes", deptTypes)
            .query(this::mapRow).list();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE id = :id")
            .param("id", id)
            .query(this::mapRow).optional()
            .orElseThrow(() -> new NotFoundException("Department not found"));
    }

    @Override
    public DepartmentResponse create(DepartmentRequest req, String actor) {
        if (departmentRepo.existsByDeptName(req.deptName()))
            throw new IllegalArgumentException("Department name already exists");
        Department dept = new Department(req.deptName(), req.description(), req.headPerson(), req.phone(), req.deptTypes());
        dept = departmentRepo.save(dept);
        return detail(dept.getId());
    }

    @Override
    public DepartmentResponse update(Long id, DepartmentRequest req, String actor) {
        Department dept = departmentRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Department not found"));
        if (departmentRepo.existsByDeptNameAndIdNot(req.deptName(), id))
            throw new IllegalArgumentException("Department name already exists");
        dept.setDeptName(req.deptName());
        dept.setDescription(req.description());
        dept.setHeadPerson(req.headPerson());
        dept.setPhone(req.phone());
        dept.setDeptTypes(req.deptTypes());
        departmentRepo.save(dept);
        return detail(id);
    }

    @Override
    public void delete(Long id, String actor) {
        if (!departmentRepo.existsById(id))
            throw new NotFoundException("Department not found");
        departmentRepo.deleteById(id);
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private DepartmentResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer deptTypes = rs.getObject("dept_types") != null ? rs.getInt("dept_types") : null;
        return new DepartmentResponse(
            rs.getLong("id"),
            rs.getString("dept_name"),
            rs.getString("description"),
            rs.getString("head_person"),
            rs.getString("phone"),
            deptTypes,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    }
}
