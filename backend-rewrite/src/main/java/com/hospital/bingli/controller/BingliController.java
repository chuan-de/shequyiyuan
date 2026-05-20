package com.hospital.bingli.controller;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import com.hospital.bingli.dto.BingliStatusChangeRequest;
import com.hospital.bingli.dto.BingliUpsertRequest;
import com.hospital.bingli.service.BingliService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/medical-records", "/api/v1/bingli"})
public class BingliController {
    private final BingliService service;
    public BingliController(BingliService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('medical-records:read', 'bingli:read')")
    public ApiResponse<PageResponse<BingliRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) BingliStatus status,
                                                         @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(PageQueryUtils.toPage(service.list(keyword, status), page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('medical-records:read', 'bingli:read')")
    public BingliRecord detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('medical-records:write', 'bingli:write')")
    public BingliRecord create(@RequestBody @Valid BingliUpsertRequest request, Principal principal) { return service.create(request, principal == null ? "system" : principal.getName()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('medical-records:write', 'bingli:write')")
    public BingliRecord update(@PathVariable Long id, @RequestBody @Valid BingliUpsertRequest request, Principal principal) { return service.update(id, request, principal == null ? "system" : principal.getName()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('medical-records:status', 'bingli:status')")
    public BingliRecord status(@PathVariable Long id, @RequestBody @Valid BingliStatusChangeRequest request, Principal principal) { return service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName()); }
}
