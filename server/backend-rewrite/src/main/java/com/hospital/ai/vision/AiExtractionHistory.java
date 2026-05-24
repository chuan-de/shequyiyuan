package com.hospital.ai.vision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only audit row for one AI vision extraction attempt. Mirrors the
 * {@code ai_extraction_history} table from V39.
 */
@Entity
@Table(name = "ai_extraction_history")
public class AiExtractionHistory {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_FAILED  = "failed";

    public static final String SOURCE_MEDICAL_RECORD = "medical_record";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "photo_id")
    private UUID photoId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(nullable = false, length = 64)
    private String model;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawJson;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public UUID getPhotoId() { return photoId; }
    public Long getOperatorId() { return operatorId; }
    public String getModel() { return model; }
    public Map<String, Object> getRawJson() { return rawJson; }
    public BigDecimal getConfidence() { return confidence; }
    public Integer getTokensIn() { return tokensIn; }
    public Integer getTokensOut() { return tokensOut; }
    public Integer getLatencyMs() { return latencyMs; }
    public String getStatus() { return status; }
    public String getErrorMsg() { return errorMsg; }
    public Instant getCreatedAt() { return createdAt; }

    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public void setPhotoId(UUID photoId) { this.photoId = photoId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public void setModel(String model) { this.model = model; }
    public void setRawJson(Map<String, Object> rawJson) { this.rawJson = rawJson; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public void setTokensIn(Integer tokensIn) { this.tokensIn = tokensIn; }
    public void setTokensOut(Integer tokensOut) { this.tokensOut = tokensOut; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public void setStatus(String status) { this.status = status; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
