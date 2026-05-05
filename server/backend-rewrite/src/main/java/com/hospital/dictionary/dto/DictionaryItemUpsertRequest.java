package com.hospital.dictionary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DictionaryItemUpsertRequest(
    @NotBlank String dictCode,
    @NotBlank String dictName,
    @NotBlank String itemCode,
    @NotBlank String itemName,
    @Min(0) Integer sortOrder,
    Boolean enabled
) {}
