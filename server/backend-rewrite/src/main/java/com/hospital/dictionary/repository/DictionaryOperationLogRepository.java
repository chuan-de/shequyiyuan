package com.hospital.dictionary.repository;

import com.hospital.dictionary.entity.DictionaryOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryOperationLogRepository extends JpaRepository<DictionaryOperationLog, Long> {
}
