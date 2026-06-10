package com.hospital.familydoctor.controller;

import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import com.hospital.familydoctor.domain.ContractStatus;
import com.hospital.familydoctor.dto.*;
import com.hospital.familydoctor.service.ContractService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family-doctor-contracts")
public class ContractController {
    private final ContractService service;

    public ContractController(ContractService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('family-doctor-contracts:read')")
    public ApiResponse<PageResponse<ContractResponse>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long familyDoctorId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String patientName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        List<ContractResponse> all = service.list(patientId, familyDoctorId, status, patientName);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('family-doctor-contracts:read')")
    public ApiResponse<ContractResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('family-doctor-contracts:write')")
    public ApiResponse<ContractResponse> create(@RequestBody @Valid ContractCreateRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, actor(p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('family-doctor-contracts:write')")
    public ApiResponse<ContractResponse> update(@PathVariable Long id,
            @RequestBody @Valid ContractUpdateRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, actor(p)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('family-doctor-contracts:write')")
    public ApiResponse<ContractResponse> changeStatus(@PathVariable Long id,
            @RequestBody @Valid ContractStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), actor(p)));
    }

    private static String actor(Principal p) { return p == null ? "system" : p.getName(); }
}
