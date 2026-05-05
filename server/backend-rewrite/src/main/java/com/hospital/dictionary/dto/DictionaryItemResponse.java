package com.hospital.dictionary.dto;

public record DictionaryItemResponse(
        Long id,
        String dictCode,
        String dictName,
        String itemCode,
        String itemName,
        Integer sortOrder,
        Boolean enabled
) {
}
