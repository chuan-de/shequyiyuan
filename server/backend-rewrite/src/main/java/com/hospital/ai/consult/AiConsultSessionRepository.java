package com.hospital.ai.consult;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link AiConsultSession}. Every accessor is parameterised by
 * {@code userId} on purpose — there is no by-id-only lookup, so a caller in
 * the service layer cannot accidentally bypass the per-user fence.
 */
public interface AiConsultSessionRepository extends JpaRepository<AiConsultSession, Long> {

    Page<AiConsultSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<AiConsultSession> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
