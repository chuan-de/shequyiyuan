package com.hospital.ai.vision;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link AiExtractionHistory}. Always loaded — same pattern
 * as {@link com.hospital.ai.audit.AiAuditLogRepository} so the JPA bean graph
 * stays consistent regardless of {@code hospital.ai.enabled}.
 */
public interface AiExtractionHistoryRepository extends JpaRepository<AiExtractionHistory, Long> {
}
