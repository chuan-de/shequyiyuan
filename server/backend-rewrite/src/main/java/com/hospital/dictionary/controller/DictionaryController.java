package com.hospital.dictionary.controller;

import com.hospital.dictionary.dto.DictionaryGroupResponse;
import com.hospital.dictionary.dto.DictionaryItemResponse;
import com.hospital.dictionary.service.DictionaryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ResponseEntity<List<DictionaryGroupResponse>> listDictionaries() {
        return ResponseEntity.ok(dictionaryService.listDictionaries());
    }

    @GetMapping("/{dictCode}/items")
    public ResponseEntity<List<DictionaryItemResponse>> listItems(@PathVariable String dictCode) {
        return ResponseEntity.ok(dictionaryService.listItems(dictCode));
    }
}
