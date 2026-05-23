package com.hospital.department.controller;

import com.hospital.common.ApiResponse;
import com.hospital.department.dto.DepartmentRequest;
import com.hospital.department.dto.DepartmentResponse;
import com.hospital.department.service.DepartmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('departments:read')")
    public ApiResponse<List<DepartmentResponse>> list(
        @RequestParam(required = false) String deptName,
        @RequestParam(required = false) Integer deptTypes) {
        return ApiResponse.ok(service.list(deptName, deptTypes));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('departments:read')")
    public ApiResponse<DepartmentResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('departments:write')")
    public ApiResponse<DepartmentResponse> create(@RequestBody @Valid DepartmentRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('departments:write')")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id,
        @RequestBody @Valid DepartmentRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('departments:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        service.delete(id, p == null ? "system" : p.getName());
        return ApiResponse.ok(null);
    }
}
