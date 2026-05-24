package com.hospital.ai.audit;

import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.observability.TraceContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
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
    private final MeterRegistry meterRegistry;

    public AiAuditService(AiAuditLogRepository repository, ObjectProvider<MeterRegistry> meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry.getIfAvailable();
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
        AiAuditLog saved = repository.save(row);
        emitMetrics(request.getFeature(), response.model(), SUCCESS, response.latencyMs(),
                response.tokensIn(), response.tokensOut());
        return saved;
    }

    /** Failed call (HTTP error, timeout, parse error, etc.). */
    public AiAuditLog recordFailure(Long userId, ChatRequest request, long latencyMs, Throwable error) {
        AiAuditLog row = baseRow(userId, request);
        row.setStatus(FAILED);
        row.setPromptExcerpt(truncate(extractPrompt(request)));
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latencyMs));
        row.setErrorMsg(truncate(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        AiAuditLog saved = repository.save(row);
        emitMetrics(request.getFeature(), request.getModel(), FAILED, latencyMs, null, null);
        return saved;
    }

    /** Rejected by rate limiter — no upstream call was made. */
    public AiAuditLog recordRateLimited(Long userId, ChatRequest request, String reason) {
        AiAuditLog row = baseRow(userId, request);
        row.setStatus(RATE_LIMITED);
        row.setPromptExcerpt(truncate(extractPrompt(request)));
        row.setLatencyMs(0);
        row.setErrorMsg(truncate("rate_limit: " + reason));
        AiAuditLog saved = repository.save(row);
        emitMetrics(request.getFeature(), request.getModel(), RATE_LIMITED, 0, null, null);
        return saved;
    }

    private void emitMetrics(String feature, String model, String status, long latencyMs,
                             Integer tokensIn, Integer tokensOut) {
        if (meterRegistry == null) return;
        String safeFeature = feature == null ? "unknown" : feature;
        String safeModel = model == null ? "unknown" : model;
        Counter.builder("hospital_ai_calls_total")
                .tag("feature", safeFeature).tag("model", safeModel).tag("status", status)
                .register(meterRegistry).increment();
        if (SUCCESS.equals(status)) {
            Timer.builder("hospital_ai_latency_seconds")
                    .tag("feature", safeFeature).tag("model", safeModel)
                    .register(meterRegistry).record(Duration.ofMillis(latencyMs));
        }
        if (tokensIn != null && tokensIn > 0) {
            Counter.builder("hospital_ai_tokens_total")
                    .tag("feature", safeFeature).tag("model", safeModel).tag("direction", "in")
                    .register(meterRegistry).increment(tokensIn);
        }
        if (tokensOut != null && tokensOut > 0) {
            Counter.builder("hospital_ai_tokens_total")
                    .tag("feature", safeFeature).tag("model", safeModel).tag("direction", "out")
                    .register(meterRegistry).increment(tokensOut);
        }
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
