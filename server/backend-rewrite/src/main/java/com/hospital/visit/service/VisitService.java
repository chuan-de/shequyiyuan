package com.hospital.visit.service;

import com.hospital.visit.dto.VisitCreateRequest;
import com.hospital.visit.dto.VisitResponse;
import com.hospital.visit.dto.VisitUpdateRequest;
import java.util.List;

public interface VisitService {
    List<VisitResponse> list(String visitNumber, Integer keshiTypes, String patientName);
    VisitResponse detail(Long id);
    VisitResponse create(VisitCreateRequest request, String actor);
    VisitResponse update(Long id, VisitUpdateRequest request, String actor);
}
