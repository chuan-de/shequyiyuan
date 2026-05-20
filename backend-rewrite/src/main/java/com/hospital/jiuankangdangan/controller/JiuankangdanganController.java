package com.hospital.jiuankangdangan.controller;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import com.hospital.jiuankangdangan.dto.JiuankangdanganStatusChangeRequest;
import com.hospital.jiuankangdangan.dto.JiuankangdanganUpsertRequest;
import com.hospital.jiuankangdangan.service.JiuankangdanganService;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/health-records", "/api/v1/jiuankangdangan"})
public class JiuankangdanganController {
    private final JiuankangdanganService service;
    public JiuankangdanganController(JiuankangdanganService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('health-records:read', 'jiuankangdangan:read')")
    public ApiResponse<PageResponse<JiuankangdanganRecord>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) JiuankangdanganStatus status,
                                                                  @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                                                  @RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(PageQueryUtils.toPage(service.list(keyword, status), page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:read')")
    public ApiResponse<JiuankangdanganRecord> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public ApiResponse<JiuankangdanganRecord> create(@RequestBody @Valid JiuankangdanganUpsertRequest request, Principal principal) {
        return ApiResponse.ok(service.create(request, principal == null ? "system" : principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public ApiResponse<JiuankangdanganRecord> update(@PathVariable Long id, @RequestBody @Valid JiuankangdanganUpsertRequest request, Principal principal) {
        return ApiResponse.ok(service.update(id, request, principal == null ? "system" : principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('jiuankangdangan:status')")
    public ApiResponse<JiuankangdanganRecord> status(@PathVariable Long id, @RequestBody @Valid JiuankangdanganStatusChangeRequest request, Principal principal) {
        return ApiResponse.ok(service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName()));
    }
}
