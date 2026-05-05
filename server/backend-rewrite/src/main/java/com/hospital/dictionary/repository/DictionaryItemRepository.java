package com.hospital.dictionary.repository;

import com.hospital.dictionary.entity.DictionaryItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    List<DictionaryItem> findByEnabledTrueOrderByDictCodeAscSortOrderAscIdAsc();

    List<DictionaryItem> findByDictCodeAndEnabledTrueOrderBySortOrderAscIdAsc(String dictCode);
}
