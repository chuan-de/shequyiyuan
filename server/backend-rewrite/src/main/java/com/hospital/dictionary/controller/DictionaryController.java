package com.hospital.dictionary.controller;

import com.hospital.common.PageResponse;
import com.hospital.dictionary.dto.DictionaryGroupResponse;
import com.hospital.dictionary.dto.DictionaryItemResponse;
import com.hospital.dictionary.dto.DictionaryItemUpsertRequest;
import com.hospital.dictionary.service.DictionaryService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {
    private final DictionaryService service;
    public DictionaryController(DictionaryService service) { this.service = service; }

    @GetMapping("/groups")
    public ResponseEntity<List<DictionaryGroupResponse>> listDictionaries() { return ResponseEntity.ok(service.listDictionaries()); }
    @GetMapping("/{dictCode}/items")
    public ResponseEntity<List<DictionaryItemResponse>> listItems(@PathVariable String dictCode) { return ResponseEntity.ok(service.listItems(dictCode)); }
    @GetMapping
    public ResponseEntity<PageResponse<DictionaryItemResponse>> page(@RequestParam(required=false) String dictCode, @RequestParam(required=false) String itemName, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size){return ResponseEntity.ok(service.page(dictCode,itemName,page,size));}
    @GetMapping("/item/{id}")
    public ResponseEntity<DictionaryItemResponse> detail(@PathVariable Long id){ return ResponseEntity.ok(service.detail(id)); }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictionaryItemResponse> create(@Valid @RequestBody DictionaryItemUpsertRequest req, Principal principal){ return ResponseEntity.ok(service.create(req, principal.getName())); }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictionaryItemResponse> update(@PathVariable Long id,@Valid @RequestBody DictionaryItemUpsertRequest req, Principal principal){ return ResponseEntity.ok(service.update(id,req, principal.getName())); }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Principal principal){ service.deactivate(id, principal.getName()); return ResponseEntity.noContent().build(); }
}
