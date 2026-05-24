package com.hospital.ai.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Audit row for one AI provider call. Matches Flyway V38.
 *
 * <p>{@code prompt_excerpt} / {@code response_excerpt} are truncated to 500
 * chars (with a trailing "..." marker) by {@link AiAuditService} so an
 * accidental enormous prompt never blows up the audit table.</p>
 */
@Entity
@Table(name = "ai_audit_log")
public class AiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 32)
    private String feature;

    @Column(length = 64)
    private String model;

    @Column(name = "prompt_excerpt", columnDefinition = "text")
    private String promptExcerpt;

    @Column(name = "response_excerpt", columnDefinition = "text")
    private String responseExcerpt;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "error_msg", columnDefinition = "text")
    private String errorMsg;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPromptExcerpt() { return promptExcerpt; }
    public void setPromptExcerpt(String promptExcerpt) { this.promptExcerpt = promptExcerpt; }
    public String getResponseExcerpt() { return responseExcerpt; }
    public void setResponseExcerpt(String responseExcerpt) { this.responseExcerpt = responseExcerpt; }
    public Integer getTokensIn() { return tokensIn; }
    public void setTokensIn(Integer tokensIn) { this.tokensIn = tokensIn; }
    public Integer getTokensOut() { return tokensOut; }
    public void setTokensOut(Integer tokensOut) { this.tokensOut = tokensOut; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Instant getCreatedAt() { return createdAt; }
}
