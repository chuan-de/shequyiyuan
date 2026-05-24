package com.hospital.ai.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, Long> {
}
