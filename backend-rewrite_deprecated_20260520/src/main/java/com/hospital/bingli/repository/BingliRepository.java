package com.hospital.bingli.repository;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import java.util.List;
import java.util.Optional;

public interface BingliRepository {
    List<BingliRecord> findAll(String keyword, BingliStatus status);
    Optional<BingliRecord> findById(Long id);
    BingliRecord save(BingliRecord record);
}
