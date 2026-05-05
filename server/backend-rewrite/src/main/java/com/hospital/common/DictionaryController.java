package com.hospital.common;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class DictionaryController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(List.of(), "dictionary list" + (type == null ? "" : " for " + type)));
    }
}
