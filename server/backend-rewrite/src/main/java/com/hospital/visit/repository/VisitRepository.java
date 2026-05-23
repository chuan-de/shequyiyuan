package com.hospital.visit.repository;

import com.hospital.visit.domain.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<VisitRecord, Long> {
    boolean existsByVisitNumber(String visitNumber);
    boolean existsByVisitNumberAndIdNot(String visitNumber, Long id);
}
