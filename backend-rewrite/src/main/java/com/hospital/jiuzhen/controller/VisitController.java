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
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {
    private final VisitService service;
    public VisitController(VisitService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('visit:read')")
    public ApiResponse<PageResponse<VisitRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) VisitStatus status,
                                                   @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(toPage(service.list(keyword, status), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('visit:read')")
    public ApiResponse<VisitRecord> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('visit:write')")
    public ApiResponse<VisitRecord> create(@RequestBody @Valid VisitUpsertRequest req, Principal p) { return ApiResponse.ok(service.create(req, p == null ? "system" : p.getName())); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('visit:write')")
    public ApiResponse<VisitRecord> update(@PathVariable Long id, @RequestBody @Valid VisitUpsertRequest req, Principal p) { return ApiResponse.ok(service.update(id, req, p == null ? "system" : p.getName())); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('visit:status')")
    public ApiResponse<VisitRecord> status(@PathVariable Long id, @RequestBody @Valid VisitStatusChangeRequest req, Principal p) {
        return ApiResponse.ok(service.changeStatus(id, req.targetStatus(), p == null ? "system" : p.getName()));
    }

    private <T> PageResponse<T> toPage(List<T> all, int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to).stream().collect(Collectors.toList()), all.size(), safePage, safeSize);
    }
}

