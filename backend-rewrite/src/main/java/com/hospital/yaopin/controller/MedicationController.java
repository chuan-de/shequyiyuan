package com.hospital.yaopin.controller;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import com.hospital.yaopin.dto.MedicationStatusChangeRequest;
import com.hospital.yaopin.dto.MedicationUpsertRequest;
import com.hospital.yaopin.service.MedicationService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import com.hospital.common.PageQueryUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationController {
    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) { this.medicationService = medicationService; }

    @GetMapping
    @PreAuthorize("hasAuthority('medication:read')")
    public ApiResponse<PageResponse<Medication>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) MedicationStatus status,
                                                    @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        List<Medication> all = medicationService.list(keyword, status);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('medication:read')")
    public ApiResponse<Medication> detail(@PathVariable Long id) { return ApiResponse.ok(medicationService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('medication:write')")
    public ApiResponse<Medication> create(@RequestBody @Valid MedicationUpsertRequest request, Principal principal) {
        return ApiResponse.ok(medicationService.create(request, principal == null ? "system" : principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('medication:write')")
    public ApiResponse<Medication> update(@PathVariable Long id, @RequestBody @Valid MedicationUpsertRequest request, Principal principal) {
        return ApiResponse.ok(medicationService.update(id, request, principal == null ? "system" : principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('medication:status')")
    public ApiResponse<Medication> changeStatus(@PathVariable Long id, @RequestBody @Valid MedicationStatusChangeRequest request, Principal principal) {
        return ApiResponse.ok(medicationService.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName()));
    }
}
