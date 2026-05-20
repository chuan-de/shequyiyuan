package com.hospital.configmodule.controller;

import com.hospital.configmodule.domain.*;
import com.hospital.configmodule.dto.*;
import com.hospital.configmodule.service.SystemConfigService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configs")
public class SystemConfigController {
    private final SystemConfigService service;
    public SystemConfigController(SystemConfigService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('configs:read')")
    public List<SystemConfig> list(@RequestParam(required = false) String key, @RequestParam(required = false) ConfigStatus status) { return service.list(key, status); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('configs:read')")
    public SystemConfig detail(@PathVariable Long id) { return service.detail(id); }
    @PostMapping @PreAuthorize("hasAuthority('configs:write')")
    public SystemConfig create(@RequestBody @Valid SystemConfigUpsertRequest req, Principal p) { return service.create(req, p == null ? "system" : p.getName()); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('configs:write')")
    public SystemConfig update(@PathVariable Long id, @RequestBody @Valid SystemConfigUpsertRequest req, Principal p) { return service.update(id, req, p == null ? "system" : p.getName()); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('configs:status')")
    public SystemConfig status(@PathVariable Long id, @RequestBody @Valid SystemConfigStatusChangeRequest req, Principal p) { return service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName()); }
}
