package com.hospital.ai.audit;

import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.observability.TraceContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Persists one row per AI call into {@code ai_audit_log}. Excerpts are
 * truncated to 500 chars (with a trailing {@code "..."} marker) so a runaway
 * prompt can't bloat the table.
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiAuditService {

    static final int EXCERPT_MAX_CHARS = 500;
    static final String SUCCESS = "success";
    static final String FAILED = "failed";
    static final String RATE_LIMITED = "rate_limited";

    private final AiAuditLogRepository repository;

    public AiAuditService(AiAuditLogRepository repository) {
        this.repository = repository;
    }

    /** Successful call: record tokens + latency + truncated content. */
    public AiAuditLog recordSuccess(Long userId, ChatRequest request, ChatResponse response) {
        AiAuditLog row = baseRow(userId, request);
        row.setStatus(SUCCESS);
        row.setModel(response.model());
        row.setPromptExcerpt(truncate(extractPrompt(request)));
        row.setResponseExcerpt(truncate(response.content()));
        row.setTokensIn(response.tokensIn());
        row.setTokensOut(response.tokensOut());
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, response.latencyMs()));
        return repository.save(row);
    }

    /** Failed call (HTTP error, timeout, parse error, etc.). */
    public AiAuditLog recordFailure(Long userId, ChatRequest request, long latencyMs, Throwable error) {
        AiAuditLog row = baseRow(userId, request);
        row.setStatus(FAILED);
        row.setPromptExcerpt(truncate(extractPrompt(request)));
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latencyMs));
        row.setErrorMsg(truncate(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        return repository.save(row);
    }

    /** Rejected by rate limiter — no upstream call was made. */
    public AiAuditLog recordRateLimited(Long userId, ChatRequest request, String reason) {
        AiAuditLog row = baseRow(userId, request);
        row.setStatus(RATE_LIMITED);
        row.setPromptExcerpt(truncate(extractPrompt(request)));
        row.setLatencyMs(0);
        row.setErrorMsg(truncate("rate_limit: " + reason));
        return repository.save(row);
    }

    private AiAuditLog baseRow(Long userId, ChatRequest request) {
        AiAuditLog row = new AiAuditLog();
        row.setUserId(userId);
        row.setFeature(request.getFeature());
        row.setModel(request.getModel());
        row.setTraceId(TraceContext.getTraceId());
        return row;
    }

    static String extractPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : request.getMessages()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(m.getRole()).append(": ").append(m.contentAsText());
            if (sb.length() > EXCERPT_MAX_CHARS) break;
        }
        return sb.toString();
    }

    static String truncate(String text) {
        if (text == null) return null;
        if (text.length() <= EXCERPT_MAX_CHARS) return text;
        return text.substring(0, EXCERPT_MAX_CHARS) + "...";
    }
}
