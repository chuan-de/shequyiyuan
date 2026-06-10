package com.hospital.visit.controller;

import com.hospital.visit.dto.VisitCreateRequest;
import com.hospital.visit.dto.VisitResponse;
import com.hospital.visit.dto.VisitUpdateRequest;
import com.hospital.visit.service.VisitService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {
    private final VisitService service;

    public VisitController(VisitService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('visits:read')")
    public ApiResponse<PageResponse<VisitResponse>> list(
        @RequestParam(required = false) String visitNumber,
        @RequestParam(required = false) Integer keshiTypes,
        @RequestParam(required = false) String patientName,
        @RequestParam(required = false) Long patientId,
        @RequestParam(required = false) Long doctorId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(visitNumber, keshiTypes, patientName, patientId, doctorId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('visits:read')")
    public ApiResponse<VisitResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('visits:write')")
    public ApiResponse<VisitResponse> create(@RequestBody @Valid VisitCreateRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('visits:write')")
    public ApiResponse<VisitResponse> update(@PathVariable Long id,
        @RequestBody @Valid VisitUpdateRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('visits:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        service.delete(id, p == null ? "system" : p.getName());
        return ApiResponse.ok(null);
    }
}
