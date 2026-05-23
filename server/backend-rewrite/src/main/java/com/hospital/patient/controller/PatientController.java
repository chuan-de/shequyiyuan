package com.hospital.patient.controller;

import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import com.hospital.patient.dto.PatientCreateRequest;
import com.hospital.patient.dto.PatientResponse;
import com.hospital.patient.dto.PatientStatusChangeRequest;
import com.hospital.patient.dto.PatientUpdateRequest;
import com.hospital.patient.service.PatientService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('patients:read')")
    public ApiResponse<PageResponse<PatientResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        List<PatientResponse> all = patientService.list(keyword);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patients:read')")
    public ApiResponse<PatientResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(patientService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('patients:write')")
    public ApiResponse<PatientResponse> create(
            @RequestBody @Valid PatientCreateRequest request, Principal principal) {
        return ApiResponse.ok(patientService.create(request, actor(principal)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('patients:write')")
    public ApiResponse<PatientResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid PatientUpdateRequest request,
            Principal principal) {
        return ApiResponse.ok(patientService.update(id, request, actor(principal)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('patients:status')")
    public ApiResponse<PatientResponse> changeStatus(
            @PathVariable Long id,
            @RequestBody @Valid PatientStatusChangeRequest request,
            Principal principal) {
        return ApiResponse.ok(patientService.changeStatus(id, request.enabled(), actor(principal)));
    }

    private static String actor(Principal principal) {
        return principal == null ? "system" : principal.getName();
    }
}
