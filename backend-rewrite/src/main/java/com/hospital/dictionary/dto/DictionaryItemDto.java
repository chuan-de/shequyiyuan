package com.hospital.dictionary.dto;

public record DictionaryItemDto(Long id, String name, String value, Integer sortOrder, boolean enabled) {}
