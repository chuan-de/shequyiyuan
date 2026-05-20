package com.hospital.bingli.service;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import com.hospital.bingli.dto.BingliUpsertRequest;
import java.util.List;

public interface BingliService {
    List<BingliRecord> list(String keyword, BingliStatus status);
    BingliRecord detail(Long id);
    BingliRecord create(BingliUpsertRequest request, String actor);
    BingliRecord update(Long id, BingliUpsertRequest request, String actor);
    BingliRecord changeStatus(Long id, BingliStatus status, String actor);
}
