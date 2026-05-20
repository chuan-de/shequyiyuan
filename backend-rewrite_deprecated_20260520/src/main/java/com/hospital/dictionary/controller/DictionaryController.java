package com.hospital.dictionary.controller;

import com.hospital.common.ApiResponse;
import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import com.hospital.dictionary.dto.PageResponse;
import com.hospital.dictionary.service.DictionaryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {
    private final DictionaryService service;
    public DictionaryController(DictionaryService service) { this.service = service; }

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
}
