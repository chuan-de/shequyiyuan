package com.hospital.ai.ingestion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * One-shot historical backfill for the Qdrant {@code patient_knowledge}
 * collection.
 *
 * <p>Walks every medical_record / health_record / visit_record id in ascending
 * order, batches of 50, with a 200ms inter-batch pause to avoid hammering
 * Doubao's rate limit. Re-runs are safe — Qdrant point ids are derived
 * deterministically from {@code (source_type, source_id, field_key)} so
 * upsert overwrites existing points instead of creating duplicates.</p>
 *
 * <p>Triggered via {@code POST /api/v1/ai/admin/backfill?dryRun=true|false}
 * (see {@code AiAdminController}). Not on a cron — admins kick it off
 * manually after the migration.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class KnowledgeBackfillJob {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBackfillJob.class);
    static final int BATCH_SIZE = 50;
    static final long INTER_BATCH_SLEEP_MS = 200L;

    private final KnowledgeIngestionService ingestionService;

    public KnowledgeBackfillJob(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public BackfillReport run(boolean dryRun) {
        log.info("Knowledge backfill starting (dryRun={})", dryRun);
        BackfillReport report = new BackfillReport();
        runOne(KnowledgeChunk.SOURCE_MEDICAL_RECORD, ingestionService::listMedicalRecordIds,
                ingestionService::ingestMedicalRecord, dryRun, report);
        runOne(KnowledgeChunk.SOURCE_HEALTH_RECORD, ingestionService::listHealthRecordIds,
                ingestionService::ingestHealthRecord, dryRun, report);
        runOne(KnowledgeChunk.SOURCE_VISIT, ingestionService::listVisitRecordIds,
                ingestionService::ingestVisitRecord, dryRun, report);
        log.info("Knowledge backfill complete: {}", report);
        return report;
    }

    private void runOne(String sourceType,
                        BatchLister lister,
                        IngestFn ingester,
                        boolean dryRun,
                        BackfillReport report) {
        long afterId = 0L;
        while (true) {
            List<Long> ids = lister.list(afterId, BATCH_SIZE);
            if (ids.isEmpty()) break;
            for (Long id : ids) {
                if (ingestionService.isCircuitBroken(sourceType, id)) {
                    report.skipped++;
                    continue;
                }
                if (dryRun) {
                    report.wouldProcess++;
                    continue;
                }
                try {
                    KnowledgeIngestionService.IngestionOutcome outcome = ingester.ingest(id);
                    if (outcome.sourceMissing()) {
                        report.skipped++;
                    } else if (outcome.processed()) {
                        report.processed++;
                        report.totalTokens += outcome.tokensUsed();
                    } else {
                        report.skipped++;
                    }
                } catch (RuntimeException ex) {
                    report.failed++;
                    log.warn("Backfill failed for {}#{}: {}", sourceType, id, ex.toString());
                }
            }
            afterId = ids.get(ids.size() - 1);
            sleep(INTER_BATCH_SLEEP_MS);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Mutable counters; converted to {@link BackfillReportDto} for the API. */
    public static final class BackfillReport {
        public int processed;
        public int skipped;
        public int wouldProcess;
        public int failed;
        public long totalTokens;

        @Override
        public String toString() {
            return "processed=" + processed + " skipped=" + skipped
                    + " wouldProcess=" + wouldProcess + " failed=" + failed
                    + " totalTokens=" + totalTokens;
        }
    }

    /** Lambdas can't take primitive long without these helpers without boxing. */
    @FunctionalInterface
    interface BatchLister {
        List<Long> list(long afterId, int limit);
    }

    @FunctionalInterface
    interface IngestFn {
        KnowledgeIngestionService.IngestionOutcome ingest(Long id);
    }
}
