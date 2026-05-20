package com.hospital.dictionary.service;

import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import com.hospital.dictionary.dto.PageResponse;
import com.hospital.dictionary.repository.DictionaryRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DictionaryService {
    private final DictionaryRepository repository;
    public DictionaryService(DictionaryRepository repository) { this.repository = repository; }

    public List<DictionaryDto> listDictionaries() { return repository.listDictionaries(); }

    public PageResponse<DictionaryItemDto> queryItems(String dictionaryCode, String itemName, int page, int size, String sortBy, String sortDir) {
        Comparator<DictionaryItemDto> comparator = switch (sortBy == null ? "sortOrder" : sortBy) {
            case "name" -> Comparator.comparing(DictionaryItemDto::name);
            case "value" -> Comparator.comparing(DictionaryItemDto::value);
            default -> Comparator.comparing(DictionaryItemDto::sortOrder);
        };
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();

        List<DictionaryItemDto> filtered = repository.listItems(dictionaryCode).stream()
            .filter(i -> itemName == null || itemName.isBlank() || i.name().toLowerCase().contains(itemName.toLowerCase()))
            .sorted(comparator)
            .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), filtered.size(), page, size);
    }
}
