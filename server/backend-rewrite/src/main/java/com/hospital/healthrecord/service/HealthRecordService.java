package com.hospital.healthrecord.service;

import com.hospital.healthrecord.domain.HealthRecord;
import com.hospital.healthrecord.domain.HealthRecordStatus;
import com.hospital.healthrecord.dto.HealthRecordUpsertRequest;
import java.util.List;

public interface HealthRecordService {
    List<HealthRecord> list(String keyword, HealthRecordStatus status);
    HealthRecord detail(Long id);
    HealthRecord create(HealthRecordUpsertRequest request, String actor);
    HealthRecord update(Long id, HealthRecordUpsertRequest request, String actor);
    HealthRecord changeStatus(Long id, HealthRecordStatus status, String actor);
}
