package com.hospital.bingli.repository;

import com.hospital.bingli.domain.BingliRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BingliRepository extends JpaRepository<BingliRecord, Long> {
}
