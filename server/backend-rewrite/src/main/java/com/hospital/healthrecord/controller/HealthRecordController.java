package com.hospital.healthrecord.controller;

import com.hospital.healthrecord.domain.HealthRecord;
import com.hospital.healthrecord.domain.HealthRecordStatus;
import com.hospital.healthrecord.dto.HealthRecordStatusChangeRequest;
import com.hospital.healthrecord.dto.HealthRecordUpsertRequest;
import com.hospital.healthrecord.service.HealthRecordService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/health-records", "/api/v1/jiuankangdangan"})
public class HealthRecordController {
    private final HealthRecordService service;
    public HealthRecordController(HealthRecordService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('health-records:read', 'jiuankangdangan:read')")
    public ApiResponse<PageResponse<HealthRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) HealthRecordStatus status,
                                                                  @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                                  @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(PageQueryUtils.toPage(service.list(keyword, status), page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:read')")
    public ApiResponse<HealthRecord> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public ApiResponse<HealthRecord> create(@RequestBody @Valid HealthRecordUpsertRequest request, Principal principal) {
        return ApiResponse.ok(service.create(request, principal == null ? "system" : principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public ApiResponse<HealthRecord> update(@PathVariable Long id, @RequestBody @Valid HealthRecordUpsertRequest request, Principal principal) {
        return ApiResponse.ok(service.update(id, request, principal == null ? "system" : principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('jiuankangdangan:status')")
    public ApiResponse<HealthRecord> status(@PathVariable Long id, @RequestBody @Valid HealthRecordStatusChangeRequest request, Principal principal) {
        return ApiResponse.ok(service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName()));
    }
}
