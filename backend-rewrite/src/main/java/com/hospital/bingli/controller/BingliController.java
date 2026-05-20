package com.hospital.bingli.controller;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import com.hospital.bingli.dto.BingliStatusChangeRequest;
import com.hospital.bingli.dto.BingliUpsertRequest;
import com.hospital.bingli.service.BingliService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bingli")
public class BingliController {
    private final BingliService service;
    public BingliController(BingliService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('bingli:read')")
    public List<BingliRecord> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) BingliStatus status) { return service.list(keyword, status); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('bingli:read')")
    public BingliRecord detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @PreAuthorize("hasAuthority('bingli:write')")
    public BingliRecord create(@RequestBody @Valid BingliUpsertRequest request, Principal principal) { return service.create(request, principal == null ? "system" : principal.getName()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('bingli:write')")
    public BingliRecord update(@PathVariable Long id, @RequestBody @Valid BingliUpsertRequest request, Principal principal) { return service.update(id, request, principal == null ? "system" : principal.getName()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('bingli:status')")
    public BingliRecord status(@PathVariable Long id, @RequestBody @Valid BingliStatusChangeRequest request, Principal principal) { return service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName()); }
}
