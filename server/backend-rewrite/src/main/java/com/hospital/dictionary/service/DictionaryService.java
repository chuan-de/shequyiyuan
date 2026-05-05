package com.hospital.dictionary.service;

import com.hospital.dictionary.dto.DictionaryGroupResponse;
import com.hospital.dictionary.dto.DictionaryItemResponse;
import com.hospital.dictionary.entity.DictionaryItem;
import com.hospital.dictionary.repository.DictionaryItemRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DictionaryService {

    private final DictionaryItemRepository dictionaryItemRepository;

    public DictionaryService(DictionaryItemRepository dictionaryItemRepository) {
        this.dictionaryItemRepository = dictionaryItemRepository;
    }

    public List<DictionaryGroupResponse> listDictionaries() {
        List<DictionaryItem> items = dictionaryItemRepository.findByEnabledTrueOrderByDictCodeAscSortOrderAscIdAsc();

        Map<String, List<DictionaryItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(DictionaryItem::getDictCode));

        return grouped.entrySet().stream()
                .map(entry -> new DictionaryGroupResponse(
                        entry.getKey(),
                        entry.getValue().get(0).getDictName(),
                        entry.getValue().stream().map(this::toResponse).toList()
                ))
                .sorted((a, b) -> a.dictCode().compareToIgnoreCase(b.dictCode()))
                .toList();
    }

    public List<DictionaryItemResponse> listItems(String dictCode) {
        return dictionaryItemRepository.findByDictCodeAndEnabledTrueOrderBySortOrderAscIdAsc(dictCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private DictionaryItemResponse toResponse(DictionaryItem item) {
        return new DictionaryItemResponse(
                item.getId(),
                item.getDictCode(),
                item.getDictName(),
                item.getItemCode(),
                item.getItemName(),
                item.getSortOrder(),
                item.getEnabled()
        );
    }
}
