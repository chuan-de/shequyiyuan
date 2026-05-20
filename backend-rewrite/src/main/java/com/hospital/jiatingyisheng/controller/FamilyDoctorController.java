package com.hospital.jiatingyisheng.controller;

import com.hospital.jiatingyisheng.domain.*;
import com.hospital.jiatingyisheng.dto.*;
import com.hospital.jiatingyisheng.service.FamilyDoctorService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family-doctors")
public class FamilyDoctorController {
    private final FamilyDoctorService service;
    public FamilyDoctorController(FamilyDoctorService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('familyDoctor:read')")
    public ApiResponse<PageResponse<FamilyDoctorContract>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) FamilyDoctorStatus status,
                                                     @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return ApiResponse.ok(toPage(service.list(keyword, status), page, size)); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('familyDoctor:read')")
    public ApiResponse<FamilyDoctorContract> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping @PreAuthorize("hasAuthority('familyDoctor:write')")
    public ApiResponse<FamilyDoctorContract> create(@RequestBody @Valid FamilyDoctorUpsertRequest req, Principal p) { return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName())); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('familyDoctor:write')")
    public ApiResponse<FamilyDoctorContract> update(@PathVariable Long id, @RequestBody @Valid FamilyDoctorUpsertRequest req, Principal p) { return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName())); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('familyDoctor:status')")
    public ApiResponse<FamilyDoctorContract> changeStatus(@PathVariable Long id, @RequestBody @Valid FamilyDoctorStatusChangeRequest req, Principal p) { return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), req.reason(), p == null ? "system" : p.getName())); }

    private <T> PageResponse<T> toPage(List<T> all, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to).stream().collect(Collectors.toList()), all.size(), safePage, safeSize);
    }
}

