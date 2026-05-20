package com.hospital.yisheng.controller;

import com.hospital.yisheng.domain.DoctorProfile;
import com.hospital.yisheng.domain.DoctorStatus;
import com.hospital.yisheng.dto.DoctorStatusChangeRequest;
import com.hospital.yisheng.dto.DoctorUpsertRequest;
import com.hospital.yisheng.service.DoctorService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {
    private final DoctorService service;
    public DoctorController(DoctorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('doctors:read')")
    public List<DoctorProfile> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) DoctorStatus status) {
        return service.list(keyword, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:read')")
    public DoctorProfile detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @PreAuthorize("hasAuthority('doctors:write')")
    public DoctorProfile create(@RequestBody @Valid DoctorUpsertRequest req, Principal p) { return service.create(req, p == null ? "system" : p.getName()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doctors:write')")
    public DoctorProfile update(@PathVariable Long id, @RequestBody @Valid DoctorUpsertRequest req, Principal p) { return service.update(id, req, p == null ? "system" : p.getName()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('doctors:status')")
    public DoctorProfile status(@PathVariable Long id, @RequestBody @Valid DoctorStatusChangeRequest req, Principal p) {
        return service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName());
    }
}
