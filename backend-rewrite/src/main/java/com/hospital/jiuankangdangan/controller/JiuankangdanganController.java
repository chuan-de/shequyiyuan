package com.hospital.jiuankangdangan.controller;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import com.hospital.jiuankangdangan.dto.JiuankangdanganStatusChangeRequest;
import com.hospital.jiuankangdangan.dto.JiuankangdanganUpsertRequest;
import com.hospital.jiuankangdangan.service.JiuankangdanganService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jiuankangdangan")
public class JiuankangdanganController {
    private final JiuankangdanganService service;
    public JiuankangdanganController(JiuankangdanganService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('jiuankangdangan:read')")
    public List<JiuankangdanganRecord> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) JiuankangdanganStatus status) {
        return service.list(keyword, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:read')")
    public JiuankangdanganRecord detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public JiuankangdanganRecord create(@RequestBody @Valid JiuankangdanganUpsertRequest request, Principal principal) {
        return service.create(request, principal == null ? "system" : principal.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('jiuankangdangan:write')")
    public JiuankangdanganRecord update(@PathVariable Long id, @RequestBody @Valid JiuankangdanganUpsertRequest request, Principal principal) {
        return service.update(id, request, principal == null ? "system" : principal.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('jiuankangdangan:status')")
    public JiuankangdanganRecord status(@PathVariable Long id, @RequestBody @Valid JiuankangdanganStatusChangeRequest request, Principal principal) {
        return service.changeStatus(id, request.targetStatus(), principal == null ? "system" : principal.getName());
    }
}
