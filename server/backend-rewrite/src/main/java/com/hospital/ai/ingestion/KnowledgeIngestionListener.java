package com.hospital.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges write-side business events to the embedding pipeline.
 *
 * <p>Listens AFTER_COMMIT so a failing embedding call (network outage,
 * Doubao 5xx, dimension mismatch) cannot roll back the original
 * medical-record / visit write. {@code @Async} pushes the
 * work off the request thread so the user's CRUD response stays snappy.</p>
 *
 * <p>The whole component is opt-in via
 * {@code hospital.ai.features.patient-rag=true}; when the flag is false the
 * listener bean is never registered, so the publisher just discards the
 * event. That keeps the test profile (rag off) free of test-time embedding
 * calls.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class KnowledgeIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionListener.class);

    private final KnowledgeIngestionService ingestionService;

    public KnowledgeIngestionListener(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIngestRequested(KnowledgeIngestRequestedEvent event) {
        if (event == null || event.sourceId() == null || event.sourceType() == null) return;
        try {
            switch (event.sourceType()) {
                case KnowledgeChunk.SOURCE_MEDICAL_RECORD ->
                        ingestionService.ingestMedicalRecord(event.sourceId());
                case KnowledgeChunk.SOURCE_VISIT ->
                        ingestionService.ingestVisitRecord(event.sourceId());
                default ->
                        log.warn("Unknown ingest source type: {}", event.sourceType());
            }
        } catch (RuntimeException ex) {
            // The ingestion service already records to ai_dead_letter; this
            // catch keeps the async executor's pool from being polluted by
            // re-thrown exceptions that nobody handles.
            log.warn("Async ingest for {}#{} failed: {}",
                    event.sourceType(), event.sourceId(), ex.toString());
        }
    }
}
