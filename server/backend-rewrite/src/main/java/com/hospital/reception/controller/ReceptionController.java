package com.hospital.reception.controller;

import com.hospital.common.ApiResponse;
import com.hospital.common.PageQueryUtils;
import com.hospital.common.PageResponse;
import com.hospital.reception.dto.ReceptionCreateRequest;
import com.hospital.reception.dto.ReceptionResetPasswordRequest;
import com.hospital.reception.dto.ReceptionResponse;
import com.hospital.reception.dto.ReceptionStatusChangeRequest;
import com.hospital.reception.dto.ReceptionUpdateRequest;
import com.hospital.reception.service.ReceptionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receptions")
public class ReceptionController {

    private final ReceptionService receptionService;

    public ReceptionController(ReceptionService receptionService) {
        this.receptionService = receptionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receptions:read')")
    public ApiResponse<PageResponse<ReceptionResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        List<ReceptionResponse> all = receptionService.list(keyword);
        return ApiResponse.ok(PageQueryUtils.toPage(all, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('receptions:read')")
    public ApiResponse<ReceptionResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(receptionService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('receptions:write')")
    public ApiResponse<ReceptionResponse> create(
            @RequestBody @Valid ReceptionCreateRequest request, Principal principal) {
        return ApiResponse.ok(receptionService.create(request, actor(principal)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('receptions:write')")
    public ApiResponse<ReceptionResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ReceptionUpdateRequest request,
            Principal principal) {
        return ApiResponse.ok(receptionService.update(id, request, actor(principal)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('receptions:status')")
    public ApiResponse<ReceptionResponse> changeStatus(
            @PathVariable Long id,
            @RequestBody @Valid ReceptionStatusChangeRequest request,
            Principal principal) {
        return ApiResponse.ok(receptionService.changeStatus(id, request.enabled(), actor(principal)));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('receptions:reset-password')")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id,
            @RequestBody @Valid ReceptionResetPasswordRequest request) {
        receptionService.resetPassword(id, request.newPassword());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('receptions:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal principal) {
        receptionService.delete(id, actor(principal));
        return ApiResponse.ok(null);
    }

    private static String actor(Principal principal) {
        return principal == null ? "system" : principal.getName();
    }
}
