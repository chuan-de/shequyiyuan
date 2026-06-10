package com.hospital.dictionary.controller;

import com.hospital.common.ApiResponse;
import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import com.hospital.dictionary.dto.DictionaryItemUpsertRequest;
import com.hospital.common.PageResponse;
import com.hospital.dictionary.service.DictionaryService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {
    private final DictionaryService service;
    public DictionaryController(DictionaryService service) { this.service = service; }

    private static String actor(Principal p) { return p == null ? "system" : p.getName(); }

    @GetMapping
    @PreAuthorize("hasAuthority('dictionary:read')")
    public ApiResponse<List<DictionaryDto>> listDictionaries() { return ApiResponse.ok(service.listDictionaries()); }

    @GetMapping("/{dictionaryCode}/items")
    @PreAuthorize("hasAuthority('dictionary:read')")
    public ApiResponse<PageResponse<DictionaryItemDto>> listItems(@PathVariable String dictionaryCode,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(required = false) String itemName,
                                                                   @RequestParam(required = false) String sortBy,
                                                                   @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.ok(service.queryItems(dictionaryCode, itemName, page, size, sortBy, sortDir));
    }

    /**
     * 业务表单消费端点：任何已登录用户可读（性别下拉、科室下拉等都要用），
     * 仅返回启用项。字典的维护（增改/启停）仍由 dictionary:write 控制。
     */
    @GetMapping("/{dictionaryCode}/items/enabled")
    public ApiResponse<List<DictionaryItemDto>> listEnabledItems(@PathVariable String dictionaryCode) {
        return ApiResponse.ok(service.listEnabledItems(dictionaryCode));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('dictionary:write')")
    public ApiResponse<DictionaryItemDto> createItem(@RequestBody @Valid DictionaryItemUpsertRequest request, Principal principal) {
        return ApiResponse.ok(service.createItem(request, actor(principal)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('dictionary:write')")
    public ApiResponse<DictionaryItemDto> updateItem(@PathVariable Long id,
                                                     @RequestBody @Valid DictionaryItemUpsertRequest request,
                                                     Principal principal) {
        return ApiResponse.ok(service.updateItem(id, request, actor(principal)));
    }

    @PatchMapping("/items/{id}/status")
    @PreAuthorize("hasAuthority('dictionary:write')")
    public ApiResponse<DictionaryItemDto> changeItemStatus(@PathVariable Long id,
                                                           @RequestBody DictionaryItemStatusRequest request,
                                                           Principal principal) {
        return ApiResponse.ok(service.changeItemStatus(id, request.enabled(), actor(principal)));
    }

    public record DictionaryItemStatusRequest(boolean enabled) {}
}
