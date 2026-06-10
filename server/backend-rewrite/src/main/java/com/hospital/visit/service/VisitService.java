package com.hospital.visit.service;

import com.hospital.common.PageResponse;
import com.hospital.visit.dto.VisitCreateRequest;
import com.hospital.visit.dto.VisitResponse;
import com.hospital.visit.dto.VisitUpdateRequest;

public interface VisitService {
    PageResponse<VisitResponse> list(String visitNumber, Integer keshiTypes, String patientName, Long patientId, Long doctorId, int page, int size);
    VisitResponse detail(Long id);
    VisitResponse create(VisitCreateRequest request, String actor);
    VisitResponse update(Long id, VisitUpdateRequest request, String actor);
    void delete(Long id, String actor);
}
