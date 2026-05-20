package com.hospital.yaopin.controller;

import com.hospital.yaopin.domain.Medication;
import com.hospital.yaopin.domain.MedicationStatus;
import com.hospital.yaopin.dto.MedicationStatusChangeRequest;
import com.hospital.yaopin.dto.MedicationUpsertRequest;
import com.hospital.yaopin.service.MedicationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationController {
    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) { this.medicationService = medicationService; }

    @GetMapping
    @PreAuthorize("hasAuthority('medications:read')")
    public List<Medication> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) MedicationStatus status) {
        return medicationService.list(keyword, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('medications:read')")
    public Medication detail(@PathVariable Long id) { return medicationService.detail(id); }

    @PostMapping
    @PreAuthorize("hasAuthority('medications:write')")
    public Medication create(@RequestBody @Valid MedicationUpsertRequest request, Principal principal) {
        return medicationService.create(request, principal == null ? "system" : principal.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('medications:write')")
    public Medication update(@PathVariable Long id, @RequestBody @Valid MedicationUpsertRequest request, Principal principal) {
        return medicationService.update(id, request, principal == null ? "system" : principal.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('medications:status')")
    public Medication changeStatus(@PathVariable Long id, @RequestBody @Valid MedicationStatusChangeRequest request, Principal principal) {
        return medicationService.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName());
    }
}
