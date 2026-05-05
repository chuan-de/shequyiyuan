package com.hospital.dictionary.repository;

import com.hospital.dictionary.entity.DictionaryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {
    List<DictionaryItem> findByEnabledTrueOrderByDictCodeAscSortOrderAscIdAsc();
    List<DictionaryItem> findByDictCodeAndEnabledTrueOrderBySortOrderAscIdAsc(String dictCode);
    Page<DictionaryItem> findByDictCodeContainingIgnoreCaseAndItemNameContainingIgnoreCase(String dictCode, String itemName, Pageable pageable);
    Optional<DictionaryItem> findByDictCodeAndItemCode(String dictCode, String itemCode);
}
