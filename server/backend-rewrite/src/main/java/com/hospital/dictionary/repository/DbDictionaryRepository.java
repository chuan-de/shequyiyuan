package com.hospital.dictionary.repository;

import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import com.hospital.dictionary.entity.DictionaryItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * dictionary_item 表为数据源的字典仓库（替代早期硬编码的 InMemory 实现）。
 * 字典分类没有独立表：以 dict_code 去重聚合得到分类列表。
 */
@Repository
public class DbDictionaryRepository implements DictionaryRepository {

    private final DictionaryItemRepository items;

    public DbDictionaryRepository(DictionaryItemRepository items) {
        this.items = items;
    }

    @Override
    public List<DictionaryDto> listDictionaries() {
        Map<String, String> byCode = new LinkedHashMap<>();
        for (DictionaryItem item : items.findAll()) {
            byCode.putIfAbsent(item.getDictCode(), item.getDictName());
        }
        return byCode.entrySet().stream()
            .map(e -> new DictionaryDto(e.getKey(), e.getValue()))
            .toList();
    }

    @Override
    public List<DictionaryItemDto> listItems(String dictionaryCode) {
        return items.findAll().stream()
            .filter(i -> i.getDictCode().equals(dictionaryCode))
            .map(i -> new DictionaryItemDto(i.getId(), i.getItemName(), i.getItemCode(), i.getSortOrder(), Boolean.TRUE.equals(i.getEnabled())))
            .toList();
    }
}
