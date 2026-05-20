package com.hospital.jiuankangdangan.repository;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import java.util.List;
import java.util.Optional;

public interface JiuankangdanganRepository {
    List<JiuankangdanganRecord> findAll(String keyword, JiuankangdanganStatus status);
    Optional<JiuankangdanganRecord> findById(Long id);
    JiuankangdanganRecord save(JiuankangdanganRecord record);
}
