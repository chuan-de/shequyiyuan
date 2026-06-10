package com.hospital.followup.service;

import com.hospital.followup.dto.FollowupResponse;
import com.hospital.followup.dto.FollowupUpsertRequest;
import java.util.List;

public interface FollowupService {
    List<FollowupResponse> list(Long patientId, String patientName);
    FollowupResponse detail(Long id);
    FollowupResponse create(FollowupUpsertRequest request, String actor);
    FollowupResponse update(Long id, FollowupUpsertRequest request, String actor);
    void delete(Long id, String actor);
}
