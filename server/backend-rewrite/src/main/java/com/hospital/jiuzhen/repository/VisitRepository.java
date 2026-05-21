package com.hospital.jiuzhen.repository;

import com.hospital.jiuzhen.domain.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<VisitRecord, Long> {
}
