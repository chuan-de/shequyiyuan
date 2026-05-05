package com.hospital.jiatingyisheng.controller;

import com.hospital.jiatingyisheng.domain.*;
import com.hospital.jiatingyisheng.dto.*;
import com.hospital.jiatingyisheng.service.FamilyDoctorService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family-doctors")
public class FamilyDoctorController {
    private final FamilyDoctorService service;
    public FamilyDoctorController(FamilyDoctorService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('familyDoctor:read')")
    public List<FamilyDoctorContract> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) FamilyDoctorStatus status) { return service.list(keyword, status); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('familyDoctor:read')")
    public FamilyDoctorContract detail(@PathVariable Long id) { return service.detail(id); }
    @PostMapping @PreAuthorize("hasAuthority('familyDoctor:write')")
    public FamilyDoctorContract create(@RequestBody @Valid FamilyDoctorUpsertRequest req, Principal p) { return service.create(req, p == null ? "system" : p.getName()); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('familyDoctor:write')")
    public FamilyDoctorContract update(@PathVariable Long id, @RequestBody @Valid FamilyDoctorUpsertRequest req, Principal p) { return service.update(id, req, p == null ? "system" : p.getName()); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('familyDoctor:status')")
    public FamilyDoctorContract changeStatus(@PathVariable Long id, @RequestBody @Valid FamilyDoctorStatusChangeRequest req, Principal p) { return service.changeStatus(id, req.targetStatus(), req.reason(), p == null ? "system" : p.getName()); }
}
