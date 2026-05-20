package com.hospital.configmodule.controller;

import com.hospital.configmodule.domain.*;
import com.hospital.configmodule.dto.*;
import com.hospital.configmodule.service.SystemConfigService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configs")
public class SystemConfigController {
    private final SystemConfigService service;
    public SystemConfigController(SystemConfigService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('configs:read')")
    public ApiResponse<PageResponse<SystemConfig>> list(@RequestParam(required = false) String key, @RequestParam(required = false) ConfigStatus status,
                                                @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return ApiResponse.ok(toPage(service.list(key, status), page, size)); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('configs:read')")
    public ApiResponse<SystemConfig> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping @PreAuthorize("hasAuthority('configs:write')")
    public ApiResponse<SystemConfig> create(@RequestBody @Valid SystemConfigUpsertRequest req, Principal p) { return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName())); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('configs:write')")
    public ApiResponse<SystemConfig> update(@PathVariable Long id, @RequestBody @Valid SystemConfigUpsertRequest req, Principal p) { return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName())); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('configs:status')")
    public ApiResponse<SystemConfig> status(@PathVariable Long id, @RequestBody @Valid SystemConfigStatusChangeRequest req, Principal p) { return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName())); }

    private <T> PageResponse<T> toPage(List<T> all, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to).stream().collect(Collectors.toList()), all.size(), safePage, safeSize);
    }
}
