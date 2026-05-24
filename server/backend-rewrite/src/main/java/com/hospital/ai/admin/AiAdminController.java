package com.hospital.ai.admin;

import com.hospital.ai.ingestion.KnowledgeBackfillJob;
import com.hospital.common.ApiResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only AI ops surface. Currently exposes a single endpoint:
 *
 * <p>{@code POST /api/v1/ai/admin/backfill?dryRun=true|false} — kicks off the
 * one-shot historical embedding job (see {@link KnowledgeBackfillJob}). Safe
 * to re-run because of the unique constraint on
 * Qdrant collection {@code patient_knowledge} (configured via
 * {@code hospital.qdrant.collection}).</p>
 *
 * <p>Gated by {@code ai:admin} permission AND the
 * {@code hospital.ai.features.patient-rag} flag — when the feature is off
 * the controller bean is not registered, so the route 404s.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/admin")
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class AiAdminController {

    private final KnowledgeBackfillJob backfillJob;

    public AiAdminController(KnowledgeBackfillJob backfillJob) {
        this.backfillJob = backfillJob;
    }

    /**
     * Kick off the one-shot historical embedding job (see
     * {@link KnowledgeBackfillJob}). Idempotent — Qdrant point ids are
     * derived from {@code (source_type, source_id, field_key)} so re-runs
     * overwrite existing points instead of duplicating.
     */
    @PostMapping("/backfill")
    @PreAuthorize("hasAuthority('ai:admin')")
    public ApiResponse<BackfillReportDto> backfill(
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun) {
        KnowledgeBackfillJob.BackfillReport r = backfillJob.run(dryRun);
        return ApiResponse.ok(new BackfillReportDto(
                dryRun, r.processed, r.wouldProcess, r.skipped, r.failed, r.totalTokens));
    }

    public record BackfillReportDto(
            boolean dryRun,
            int processed,
            int wouldProcess,
            int skipped,
            int failed,
            long totalTokens) { }
}
