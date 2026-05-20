package com.hospital.yisheng.controller;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import com.hospital.yisheng.dto.DoctorStatusChangeRequest;
import com.hospital.yisheng.dto.DoctorUpsertRequest;
import com.hospital.yisheng.service.DoctorService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {
    private final DoctorService service;
    public DoctorController(DoctorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('doctors:read')")
    public ApiResponse<PageResponse<DoctorProfile>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) DoctorStatus status,
                                                     @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(toPage(service.list(keyword, status), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:read')")
    public ApiResponse<DoctorProfile> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('doctors:write')")
    public ApiResponse<DoctorProfile> create(@RequestBody @Valid DoctorUpsertRequest req, Principal p) { return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName())); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:write')")
    public ApiResponse<DoctorProfile> update(@PathVariable Long id, @RequestBody @Valid DoctorUpsertRequest req, Principal p) { return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName())); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('doctors:status')")
    public ApiResponse<DoctorProfile> status(@PathVariable Long id, @RequestBody @Valid DoctorStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName()));
    }

    private <T> PageResponse<T> toPage(List<T> all, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to).stream().collect(Collectors.toList()), all.size(), safePage, safeSize);
    }
}

