package com.hospital.followup.service;

import com.hospital.common.PageResponse;
import com.hospital.followup.dto.FollowupResponse;
import com.hospital.followup.dto.FollowupUpsertRequest;

public interface FollowupService {
    PageResponse<FollowupResponse> list(Long patientId, String patientName, int page, int size);
    FollowupResponse detail(Long id);
    FollowupResponse create(FollowupUpsertRequest request, String actor);
    FollowupResponse update(Long id, FollowupUpsertRequest request, String actor);
    void delete(Long id, String actor);
}
