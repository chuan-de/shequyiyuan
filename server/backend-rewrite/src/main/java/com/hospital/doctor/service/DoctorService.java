package com.hospital.doctor.service;

import com.hospital.doctor.dto.*;
import java.util.List;

public interface DoctorService {
    List<DoctorResponse> list(String keyword, String uuidNumber, String fullName, Integer sexTypes);
    DoctorResponse detail(Long id);
    DoctorResponse create(DoctorCreateRequest request, String actor);
    DoctorResponse update(Long id, DoctorUpdateRequest request, String actor);
    DoctorResponse changeStatus(Long id, boolean enabled, String actor);
    void resetPassword(Long id, String newPassword, String actor);
    void delete(Long id, String actor);
}
