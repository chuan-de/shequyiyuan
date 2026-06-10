package com.hospital.followup.controller;

import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import com.hospital.followup.dto.FollowupResponse;
import com.hospital.followup.dto.FollowupUpsertRequest;
import com.hospital.followup.service.FollowupService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/followups")
public class FollowupController {
    private final FollowupService service;

    public FollowupController(FollowupService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('followups:read')")
    public ApiResponse<PageResponse<FollowupResponse>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String patientName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        List<FollowupResponse> all = service.list(patientId, patientName);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('followups:read')")
    public ApiResponse<FollowupResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('followups:write')")
    public ApiResponse<FollowupResponse> create(@RequestBody @Valid FollowupUpsertRequest req, Principal p) {
        return ApiResponse.ok(service.create(req, actor(p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('followups:write')")
    public ApiResponse<FollowupResponse> update(@PathVariable Long id,
            @RequestBody @Valid FollowupUpsertRequest req, Principal p) {
        return ApiResponse.ok(service.update(id, req, actor(p)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('followups:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        service.delete(id, actor(p));
        return ApiResponse.ok(null);
    }

    private static String actor(Principal p) { return p == null ? "system" : p.getName(); }
}
