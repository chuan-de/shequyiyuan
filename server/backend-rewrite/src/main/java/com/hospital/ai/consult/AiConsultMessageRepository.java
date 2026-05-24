package com.hospital.ai.consult;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConsultMessageRepository extends JpaRepository<AiConsultMessage, Long> {

    List<AiConsultMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}
