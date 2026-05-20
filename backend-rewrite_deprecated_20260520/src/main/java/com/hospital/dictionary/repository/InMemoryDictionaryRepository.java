package com.hospital.dictionary.repository;

import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDictionaryRepository implements DictionaryRepository {
    private static final List<DictionaryDto> DICTS = List.of(
        new DictionaryDto("doctor_status", "医生状态"),
        new DictionaryDto("gender", "性别"),
        new DictionaryDto("visit_status", "就诊状态")
    );

    private static final Map<String, List<DictionaryItemDto>> ITEMS = Map.of(
        "doctor_status", List.of(new DictionaryItemDto(1L, "待审核", "PENDING", 1, true), new DictionaryItemDto(2L, "在岗", "ACTIVE", 2, true), new DictionaryItemDto(3L, "停用", "DISABLED", 3, true)),
        "gender", List.of(new DictionaryItemDto(4L, "男", "M", 1, true), new DictionaryItemDto(5L, "女", "F", 2, true)),
        "visit_status", List.of(new DictionaryItemDto(6L, "待就诊", "PENDING", 1, true), new DictionaryItemDto(7L, "已完成", "DONE", 2, true))
    );

    @Override public List<DictionaryDto> listDictionaries() { return DICTS; }
    @Override public List<DictionaryItemDto> listItems(String dictionaryCode) { return ITEMS.getOrDefault(dictionaryCode, List.of()); }
}
