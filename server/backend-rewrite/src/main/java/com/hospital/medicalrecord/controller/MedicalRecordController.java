package com.hospital.medicalrecord.controller;

import com.hospital.medicalrecord.domain.MedicalRecord;
import com.hospital.medicalrecord.domain.MedicalRecordStatus;
import com.hospital.medicalrecord.dto.MedicalRecordStatusChangeRequest;
import com.hospital.medicalrecord.dto.MedicalRecordUpsertRequest;
import com.hospital.medicalrecord.service.MedicalRecordService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/medical-records", "/api/v1/bingli"})
public class MedicalRecordController {
    private final MedicalRecordService service;
    public MedicalRecordController(MedicalRecordService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('medical-records:read', 'bingli:read')")
    public ApiResponse<PageResponse<MedicalRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) MedicalRecordStatus status,
                                                         @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(PageQueryUtils.toPage(service.list(keyword, status), page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('medical-records:read', 'bingli:read')")
    public ApiResponse<MedicalRecord> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('medical-records:write', 'bingli:write')")
    public ApiResponse<MedicalRecord> create(@RequestBody @Valid MedicalRecordUpsertRequest request, Principal principal) { return ApiResponse.ok(service.create(request, principal == null ? "system" : principal.getName())); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('medical-records:write', 'bingli:write')")
    public ApiResponse<MedicalRecord> update(@PathVariable Long id, @RequestBody @Valid MedicalRecordUpsertRequest request, Principal principal) { return ApiResponse.ok(service.update(id, request, principal == null ? "system" : principal.getName())); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('medical-records:status', 'bingli:status')")
    public ApiResponse<MedicalRecord> status(@PathVariable Long id, @RequestBody @Valid MedicalRecordStatusChangeRequest request, Principal principal) { return ApiResponse.ok(service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName())); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('medical-records:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal == null ? "system" : principal.getName());
        return ApiResponse.ok(null);
    }
}
