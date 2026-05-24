package com.hospital.doctor.controller;

import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import com.hospital.doctor.dto.*;
import com.hospital.doctor.service.DoctorService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService service;

    public DoctorController(DoctorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('doctors:read')")
    public ApiResponse<PageResponse<DoctorResponse>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String uuidNumber,
        @RequestParam(required = false) String fullName,
        @RequestParam(required = false) Integer sexTypes,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {
        List<DoctorResponse> all = service.list(keyword, uuidNumber, fullName, sexTypes);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:read')")
    public ApiResponse<DoctorResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('doctors:write')")
    public ApiResponse<DoctorResponse> create(@RequestBody @Valid DoctorCreateRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:write')")
    public ApiResponse<DoctorResponse> update(@PathVariable Long id,
        @RequestBody @Valid DoctorUpdateRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('doctors:status')")
    public ApiResponse<DoctorResponse> changeStatus(@PathVariable Long id,
        @RequestBody @Valid DoctorStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.enabled(), p == null ? "system" : p.getName()));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('doctors:reset-password')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
        @RequestBody @Valid DoctorResetPasswordRequest req, Principal p) {
        service.resetPassword(id, req.newPassword(), p == null ? "system" : p.getName());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        service.delete(id, p == null ? "system" : p.getName());
        return ApiResponse.ok(null);
    }
}
