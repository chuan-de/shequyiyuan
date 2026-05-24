package com.hospital.ai.ingestion;

/**
 * Application event signalling that a source row (medical_record /
 * health_record / visit) has been written or updated and should be
 * re-embedded into {@code patient_knowledge_chunk}.
 *
 * <p>Published from business services AFTER the writing transaction commits
 * (via {@code ApplicationEventPublisher}). The listener
 * {@link KnowledgeIngestionListener} is annotated
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} and {@code @Async}
 * so ingestion failures cannot roll back the business transaction.</p>
 */
public record KnowledgeIngestRequestedEvent(String sourceType, Long sourceId) {

    public static KnowledgeIngestRequestedEvent medicalRecord(Long id) {
        return new KnowledgeIngestRequestedEvent(KnowledgeChunk.SOURCE_MEDICAL_RECORD, id);
    }

    public static KnowledgeIngestRequestedEvent healthRecord(Long id) {
        return new KnowledgeIngestRequestedEvent(KnowledgeChunk.SOURCE_HEALTH_RECORD, id);
    }

    public static KnowledgeIngestRequestedEvent visit(Long id) {
        return new KnowledgeIngestRequestedEvent(KnowledgeChunk.SOURCE_VISIT, id);
    }
}
