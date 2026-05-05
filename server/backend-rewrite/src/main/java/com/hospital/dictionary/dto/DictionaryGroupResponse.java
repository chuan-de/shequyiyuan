package com.hospital.dictionary.dto;

import java.util.List;

public record DictionaryGroupResponse(
        String dictCode,
        String dictName,
        List<DictionaryItemResponse> items
) {
}
