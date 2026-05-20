package com.hospital.jiuzhen.controller;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import com.hospital.jiuzhen.dto.VisitStatusChangeRequest;
import com.hospital.jiuzhen.dto.VisitUpsertRequest;
import com.hospital.jiuzhen.service.VisitService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import com.hospital.common.PageQueryUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {
    private final VisitService service;
    public VisitController(VisitService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('visits:read')")
    public ApiResponse<PageResponse<VisitRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) VisitStatus status,
                                                   @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(PageQueryUtils.toPage(service.list(keyword, status), page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('visits:read')")
    public ApiResponse<VisitRecord> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('visits:write')")
    public ApiResponse<VisitRecord> create(@RequestBody @Valid VisitUpsertRequest req, Principal p) { return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName())); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('visits:write')")
    public ApiResponse<VisitRecord> update(@PathVariable Long id, @RequestBody @Valid VisitUpsertRequest req, Principal p) { return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName())); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('visits:status')")
    public ApiResponse<VisitRecord> status(@PathVariable Long id, @RequestBody @Valid VisitStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName()));
    }
}

