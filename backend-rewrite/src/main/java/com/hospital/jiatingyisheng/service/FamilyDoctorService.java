package com.hospital.jiatingyisheng.service;

import com.hospital.jiatingyisheng.domain.*;
import com.hospital.jiatingyisheng.dto.*;
import java.util.List;

public interface FamilyDoctorService {
    List<FamilyDoctorContract> list(String keyword, FamilyDoctorStatus status);
    FamilyDoctorContract detail(Long id);
    FamilyDoctorContract create(FamilyDoctorUpsertRequest request, String actor);
    FamilyDoctorContract update(Long id, FamilyDoctorUpsertRequest request, String actor);
    FamilyDoctorContract changeStatus(Long id, FamilyDoctorStatus targetStatus, String reason, String actor);
}
