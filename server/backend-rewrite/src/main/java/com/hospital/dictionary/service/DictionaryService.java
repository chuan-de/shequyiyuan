package com.hospital.dictionary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.PageResponse;
import com.hospital.common.ExportRequest;
import com.hospital.common.ExportResponse;
import com.hospital.dictionary.dto.DictionaryGroupResponse;
import com.hospital.dictionary.dto.DictionaryItemResponse;
import com.hospital.dictionary.dto.DictionaryItemUpsertRequest;
import com.hospital.dictionary.entity.DictionaryItem;
import com.hospital.dictionary.entity.DictionaryOperationLog;
import com.hospital.dictionary.repository.DictionaryItemRepository;
import com.hospital.dictionary.repository.DictionaryOperationLogRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryService {
    private final DictionaryItemRepository repo;
    private final DictionaryOperationLogRepository logRepo;
    private final ObjectMapper objectMapper;

    public DictionaryService(DictionaryItemRepository repo, DictionaryOperationLogRepository logRepo, ObjectMapper objectMapper) {
        this.repo = repo; this.logRepo = logRepo; this.objectMapper = objectMapper;
    }
    public List<DictionaryGroupResponse> listDictionaries() { /* existing */
        List<DictionaryItem> items = repo.findByEnabledTrueOrderByDictCodeAscSortOrderAscIdAsc();
        Map<String, List<DictionaryItem>> grouped = items.stream().collect(Collectors.groupingBy(DictionaryItem::getDictCode));
        return grouped.entrySet().stream().map(e -> new DictionaryGroupResponse(e.getKey(), e.getValue().get(0).getDictName(), e.getValue().stream().map(this::toResponse).toList())).toList();
    }
    public List<DictionaryItemResponse> listItems(String dictCode) { return repo.findByDictCodeAndEnabledTrueOrderBySortOrderAscIdAsc(dictCode).stream().map(this::toResponse).toList(); }

    public PageResponse<DictionaryItemResponse> page(String dictCode, String itemName, Boolean enabled, int page, int size, String sortBy, String sortDir) {
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        var p = repo.findByDictCodeContainingIgnoreCaseAndItemNameContainingIgnoreCase(dictCode == null ? "" : dictCode, itemName == null ? "" : itemName,
                PageRequest.of(page - 1, size, sort.and(Sort.by("id").ascending())));
        var records = p.getContent().stream().filter(item -> enabled == null || item.getEnabled().equals(enabled)).map(this::toResponse).toList();
        return new PageResponse<>(records, p.getTotalElements(), page, size);
    }

    public ExportResponse export(ExportRequest req) {
        List<String> whitelist = List.of("dictCode", "dictName", "itemCode", "itemName", "sortOrder", "enabled");
        if (!whitelist.containsAll(req.fields())) {
            throw new IllegalArgumentException("Export fields contain non-whitelisted columns");
        }
        if (req.async()) {
            return ExportResponse.queued("dict-exp-" + System.currentTimeMillis());
        }
        return ExportResponse.ready("/api/v1/dictionaries/export/download/latest");
    }

    public DictionaryItemResponse detail(Long id) { return toResponse(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary item not found"))); }

    @Transactional
    public DictionaryItemResponse create(DictionaryItemUpsertRequest req, String operator) {
        repo.findByDictCodeAndItemCode(req.dictCode(), req.itemCode()).ifPresent(i -> { throw new IllegalArgumentException("Dictionary item already exists"); });
        DictionaryItem item = apply(new DictionaryItem(), req); item = repo.save(item);
        saveLog(item.getId(), "CREATE", operator, null, item);
        return toResponse(item);
    }
    @Transactional
    public DictionaryItemResponse update(Long id, DictionaryItemUpsertRequest req, String operator) {
        DictionaryItem item = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary item not found"));
        String before = toJson(item); apply(item, req); item = repo.save(item); saveLog(id, "UPDATE", operator, before, item); return toResponse(item);
    }
    @Transactional
    public void deactivate(Long id, String operator) {
        DictionaryItem item = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary item not found"));
        String before = toJson(item); item.setEnabled(false); repo.save(item); saveLog(id, "DEACTIVATE", operator, before, item);
    }

    private DictionaryItem apply(DictionaryItem i, DictionaryItemUpsertRequest r){ i.setDictCode(r.dictCode()); i.setDictName(r.dictName()); i.setItemCode(r.itemCode()); i.setItemName(r.itemName()); i.setSortOrder(r.sortOrder()); i.setEnabled(r.enabled()==null?true:r.enabled()); return i;}
    private DictionaryItemResponse toResponse(DictionaryItem item) { return new DictionaryItemResponse(item.getId(), item.getDictCode(), item.getDictName(), item.getItemCode(), item.getItemName(), item.getSortOrder(), item.getEnabled()); }
    private void saveLog(Long itemId,String op,String operator,String before,DictionaryItem after){ logRepo.save(DictionaryOperationLog.of(itemId, op, operator, before, toJson(after))); }
    private String toJson(DictionaryItem item){ try { return objectMapper.writeValueAsString(toResponse(item)); } catch (JsonProcessingException e) { return "{}"; } }
}
