package com.hospital.jiuzhen.service;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import com.hospital.jiuzhen.dto.VisitUpsertRequest;
import java.util.List;

public interface VisitService {
    List<VisitRecord> list(String keyword, VisitStatus status);
    VisitRecord detail(Long id);
    VisitRecord create(VisitUpsertRequest request, String actor);
    VisitRecord update(Long id, VisitUpsertRequest request, String actor);
    VisitRecord changeStatus(Long id, VisitStatus status, String actor);
}
