package com.hospital.familydoctor.controller;

import com.hospital.familydoctor.dto.*;
import com.hospital.familydoctor.service.FamilyDoctorService;
import com.hospital.common.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family-doctors")
public class FamilyDoctorController {
    private final FamilyDoctorService service;

    public FamilyDoctorController(FamilyDoctorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('family-doctors:read')")
    public ApiResponse<List<FamilyDoctorResponse>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String fullName,
        @RequestParam(required = false) Integer sexTypes) {
        return ApiResponse.ok(service.list(keyword, fullName, sexTypes));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('family-doctors:read')")
    public ApiResponse<FamilyDoctorResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('family-doctors:write')")
    public ApiResponse<FamilyDoctorResponse> create(@RequestBody @Valid FamilyDoctorCreateRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('family-doctors:write')")
    public ApiResponse<FamilyDoctorResponse> update(@PathVariable Long id,
        @RequestBody @Valid FamilyDoctorUpdateRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('family-doctors:status')")
    public ApiResponse<FamilyDoctorResponse> changeStatus(@PathVariable Long id,
        @RequestBody @Valid FamilyDoctorStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.enabled(), p == null ? "system" : p.getName()));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('family-doctors:reset-password')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
        @RequestBody @Valid FamilyDoctorResetPasswordRequest req, Principal p) {
        service.resetPassword(id, req.newPassword(), p == null ? "system" : p.getName());
        return ApiResponse.ok(null);
    }
}
