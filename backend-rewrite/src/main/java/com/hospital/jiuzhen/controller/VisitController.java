package com.hospital.jiuzhen.controller;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import com.hospital.jiuzhen.dto.VisitStatusChangeRequest;
import com.hospital.jiuzhen.dto.VisitUpsertRequest;
import com.hospital.jiuzhen.service.VisitService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {
    private final VisitService service;
    public VisitController(VisitService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('visit:read')")
    public List<VisitRecord> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) VisitStatus status) {
        return service.list(keyword, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('visit:read')")
    public VisitRecord detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @PreAuthorize("hasAuthority('visit:write')")
    public VisitRecord create(@RequestBody @Valid VisitUpsertRequest req, Principal p) { return service.create(req, p == null ? "system" : p.getName()); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('visit:write')")
    public VisitRecord update(@PathVariable Long id, @RequestBody @Valid VisitUpsertRequest req, Principal p) { return service.update(id, req, p == null ? "system" : p.getName()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('visit:status')")
    public VisitRecord status(@PathVariable Long id, @RequestBody @Valid VisitStatusChangeRequest req, Principal p) {
        return service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName());
    }
}
