package com.hospital.reception.repository;

import com.hospital.reception.domain.ReceptionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceptionProfileRepository extends JpaRepository<ReceptionProfile, Long> {
    boolean existsByPhone(String phone);
    boolean existsByUuidNumber(String uuidNumber);

    @Query("SELECT COUNT(r) > 0 FROM ReceptionProfile r WHERE r.phone = :phone AND r.id <> :id")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("id") Long id);

    @Query("SELECT COUNT(r) > 0 FROM ReceptionProfile r WHERE r.uuidNumber = :uuidNumber AND r.id <> :id")
    boolean existsByUuidNumberAndIdNot(@Param("uuidNumber") String uuidNumber, @Param("id") Long id);
}
