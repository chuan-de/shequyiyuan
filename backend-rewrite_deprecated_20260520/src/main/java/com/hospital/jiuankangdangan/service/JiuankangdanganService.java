package com.hospital.jiuankangdangan.service;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import com.hospital.jiuankangdangan.dto.JiuankangdanganUpsertRequest;
import java.util.List;

public interface JiuankangdanganService {
    List<JiuankangdanganRecord> list(String keyword, JiuankangdanganStatus status);
    JiuankangdanganRecord detail(Long id);
    JiuankangdanganRecord create(JiuankangdanganUpsertRequest request, String actor);
    JiuankangdanganRecord update(Long id, JiuankangdanganUpsertRequest request, String actor);
    JiuankangdanganRecord changeStatus(Long id, JiuankangdanganStatus status, String actor);
}
