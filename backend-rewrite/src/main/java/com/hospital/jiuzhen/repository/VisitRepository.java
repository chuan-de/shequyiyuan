package com.hospital.jiuzhen.repository;

import com.hospital.jiuzhen.domain.VisitRecord;
import com.hospital.jiuzhen.domain.VisitStatus;
import java.util.List;
import java.util.Optional;

public interface VisitRepository {
    List<VisitRecord> findAll(String keyword, VisitStatus status);
    Optional<VisitRecord> findById(Long id);
    VisitRecord save(VisitRecord visit);
}
