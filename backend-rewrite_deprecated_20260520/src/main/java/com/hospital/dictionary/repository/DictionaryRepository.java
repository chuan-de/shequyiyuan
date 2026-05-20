package com.hospital.dictionary.repository;

import com.hospital.dictionary.dto.DictionaryDto;
import com.hospital.dictionary.dto.DictionaryItemDto;
import java.util.List;

public interface DictionaryRepository {
    List<DictionaryDto> listDictionaries();
    List<DictionaryItemDto> listItems(String dictionaryCode);
}
