package com.hospital.dictionary.service;

import com.hospital.common.NotFoundException;
import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import com.hospital.dictionary.dto.DictionaryItemUpsertRequest;
import com.hospital.dictionary.dto.PageResponse;
import com.hospital.dictionary.entity.DictionaryItem;
import com.hospital.dictionary.entity.DictionaryOperationLog;
import com.hospital.dictionary.repository.DictionaryItemRepository;
import com.hospital.dictionary.repository.DictionaryOperationLogRepository;
import com.hospital.dictionary.repository.DictionaryRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryService {
    private final DictionaryRepository repository;
    private final DictionaryItemRepository itemRepository;
    private final DictionaryOperationLogRepository operationLogRepository;

    public DictionaryService(DictionaryRepository repository,
                             DictionaryItemRepository itemRepository,
                             DictionaryOperationLogRepository operationLogRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.operationLogRepository = operationLogRepository;
    }

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

    /** 业务表单消费：仅启用项，按 sort_order 排序。所有登录用户可读。 */
    public List<DictionaryItemDto> listEnabledItems(String dictionaryCode) {
        return itemRepository.findByDictCodeAndEnabledTrueOrderBySortOrderAscIdAsc(dictionaryCode).stream()
            .map(i -> new DictionaryItemDto(i.getId(), i.getItemName(), i.getItemCode(), i.getSortOrder(), true))
            .toList();
    }

    @Transactional
    public DictionaryItemDto createItem(DictionaryItemUpsertRequest request, String operator) {
        itemRepository.findByDictCodeAndItemCode(request.dictCode(), request.itemCode())
            .ifPresent(existing -> {
                throw new IllegalArgumentException("字典项编码已存在：" + request.dictCode() + "/" + request.itemCode());
            });
        DictionaryItem item = new DictionaryItem();
        applyRequest(item, request);
        DictionaryItem saved = itemRepository.save(item);
        operationLogRepository.save(DictionaryOperationLog.of(saved.getId(), "CREATE", operator, null, describe(saved)));
        return toDto(saved);
    }

    @Transactional
    public DictionaryItemDto updateItem(Long id, DictionaryItemUpsertRequest request, String operator) {
        DictionaryItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("字典项不存在：" + id));
        itemRepository.findByDictCodeAndItemCode(request.dictCode(), request.itemCode())
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw new IllegalArgumentException("字典项编码已存在：" + request.dictCode() + "/" + request.itemCode());
            });
        String before = describe(item);
        applyRequest(item, request);
        DictionaryItem saved = itemRepository.save(item);
        operationLogRepository.save(DictionaryOperationLog.of(saved.getId(), "UPDATE", operator, before, describe(saved)));
        return toDto(saved);
    }

    @Transactional
    public DictionaryItemDto changeItemStatus(Long id, boolean enabled, String operator) {
        DictionaryItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("字典项不存在：" + id));
        String before = describe(item);
        item.setEnabled(enabled);
        DictionaryItem saved = itemRepository.save(item);
        operationLogRepository.save(DictionaryOperationLog.of(saved.getId(), enabled ? "ENABLE" : "DISABLE", operator, before, describe(saved)));
        return toDto(saved);
    }

    private static void applyRequest(DictionaryItem item, DictionaryItemUpsertRequest request) {
        item.setDictCode(request.dictCode());
        item.setDictName(request.dictName());
        item.setItemCode(request.itemCode());
        item.setItemName(request.itemName());
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        item.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
    }

    private static DictionaryItemDto toDto(DictionaryItem item) {
        return new DictionaryItemDto(item.getId(), item.getItemName(), item.getItemCode(), item.getSortOrder(), Boolean.TRUE.equals(item.getEnabled()));
    }

    private static String describe(DictionaryItem item) {
        return item.getDictCode() + "/" + item.getItemCode() + "=" + item.getItemName()
            + " sort=" + item.getSortOrder() + " enabled=" + item.getEnabled();
    }
}
