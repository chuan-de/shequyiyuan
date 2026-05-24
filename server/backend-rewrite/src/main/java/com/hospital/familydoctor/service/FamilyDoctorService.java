package com.hospital.familydoctor.service;

import com.hospital.familydoctor.dto.*;
import java.util.List;

public interface FamilyDoctorService {
    List<FamilyDoctorResponse> list(String keyword, String fullName, Integer sexTypes);
    FamilyDoctorResponse detail(Long id);
    FamilyDoctorResponse create(FamilyDoctorCreateRequest request, String actor);
    FamilyDoctorResponse update(Long id, FamilyDoctorUpdateRequest request, String actor);
    FamilyDoctorResponse changeStatus(Long id, boolean enabled, String actor);
    void resetPassword(Long id, String newPassword, String actor);
    void delete(Long id, String actor);
}
