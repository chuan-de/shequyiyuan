package com.hospital.reception.service;

import com.hospital.reception.dto.ReceptionCreateRequest;
import com.hospital.reception.dto.ReceptionResponse;
import com.hospital.reception.dto.ReceptionUpdateRequest;
import java.util.List;

public interface ReceptionService {
    List<ReceptionResponse> list(String keyword);
    ReceptionResponse detail(Long id);
    ReceptionResponse create(ReceptionCreateRequest request, String actor);
    ReceptionResponse update(Long id, ReceptionUpdateRequest request, String actor);
    ReceptionResponse changeStatus(Long id, boolean enabled, String actor);
    void resetPassword(Long id, String newPassword);
}
