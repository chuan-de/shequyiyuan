package com.hospital.ai.embedding;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hospital.ai.audit.AiAuditLog;
import com.hospital.ai.audit.AiAuditLogRepository;
import com.hospital.ai.audit.CurrentUserResolver;
import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;
import com.hospital.ai.ratelimit.AiRateLimiter;
import com.hospital.observability.TraceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Doubao (Volcengine Ark) text-embedding implementation.
 *
 * <p>Talks to the OpenAI-compatible {@code /v1/embeddings} endpoint. Uses the
 * same shared {@link RestClient} ("aiRestClient") and {@link AiProperties}
 * config as {@code AiCallTemplate} so timeout/auth/retry behaviour stays
 * consistent — but we deliberately do NOT route through
 * {@code AiCallTemplate.chat()} because that endpoint is /chat/completions
 * with a different request/response shape. Spring AI's
 * {@code EmbeddingModel} auto-config is disabled (see
 * {@code spring.ai.model.embedding=none}) for the same reason: we want the
 * temperature-1 / retry / audit story unified.</p>
 *
 * <p>Rate limit + audit are wired here explicitly (not via
 * {@code AiAuditInterceptor}, which only matches
 * {@code AiCallTemplate.chat(..)}). Each call:</p>
 * <ol>
 *   <li>Reserves quota via {@link AiRateLimiter#acquireOrThrow(Long, int)} —
 *       throws {@link AiRateLimitException} (HTTP 429).</li>
 *   <li>POSTs the batch with up to 3 retries on 429/5xx (exponential backoff
 *       1s × 2^n).</li>
 *   <li>Persists one {@link AiAuditLog} row with feature={@code embedding}.</li>
 *   <li>Fails fast if any returned vector has a dimension other than
 *       {@link AiProperties#getEmbeddingDimension()} — the {@code VECTOR(N)}
 *       column would otherwise reject the insert with a less helpful error.</li>
 * </ol>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class DoubaoEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DoubaoEmbeddingService.class);
    private static final String FEATURE = "embedding";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final AiProperties properties;
    private final AiRateLimiter rateLimiter;
    private final AiAuditLogRepository auditRepository;
    private final CurrentUserResolver currentUserResolver;

    public DoubaoEmbeddingService(@Qualifier("aiRestClient") RestClient restClient,
                                  AiProperties properties,
                                  AiRateLimiter rateLimiter,
                                  AiAuditLogRepository auditRepository,
                                  CurrentUserResolver currentUserResolver) {
        this.restClient = restClient;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.auditRepository = auditRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public EmbeddingBatchResult embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingBatchResult(List.of(), 0, 0, 0);
        }

        Long userId = currentUserResolver.currentUserId();
        // Pre-estimate: roughly 1 token per 2 chars (Chinese skews lower, but
        // Doubao's tokenizer averages out for our medical-record corpus). Cap
        // the estimate so very long batches don't immediately exhaust quota.
        int estimate = Math.min(8000, totalChars(texts) / 2);
        try {
            rateLimiter.acquireOrThrow(userId, estimate);
        } catch (AiRateLimitException ex) {
            recordRateLimited(userId, texts, ex.getReason());
            throw ex;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getEmbeddingModel());
        body.put("input", texts);
        body.put("encoding_format", "float");

        long start = System.currentTimeMillis();
        RestClientResponseException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri(properties.getBaseUrl() + "/embeddings")
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                long latency = System.currentTimeMillis() - start;
                EmbeddingBatchResult result = parse(response, texts.size(), latency);
                rateLimiter.debitActualTokens(userId, result.totalTokens() - estimate);
                recordSuccess(userId, texts, result);
                return result;
            } catch (RestClientResponseException ex) {
                lastError = ex;
                HttpStatusCode status = ex.getStatusCode();
                if (!isRetryable(status) || attempt == MAX_ATTEMPTS) {
                    long latency = System.currentTimeMillis() - start;
                    recordFailure(userId, texts, latency, ex);
                    throw ex;
                }
                Duration backoff = BASE_BACKOFF.multipliedBy(1L << (attempt - 1));
                log.warn("Embedding attempt {} failed with {} — retrying in {}ms",
                        attempt, status.value(), backoff.toMillis());
                sleep(backoff);
            } catch (RuntimeException ex) {
                long latency = System.currentTimeMillis() - start;
                recordFailure(userId, texts, latency, ex);
                throw ex;
            }
        }
        // Defensive — loop only exits via return or throw above.
        throw lastError != null ? lastError : new IllegalStateException("unreachable");
    }

    // --- response parsing --------------------------------------------------

    @SuppressWarnings("unchecked")
    EmbeddingBatchResult parse(Map<String, Object> response, int expectedCount, long latencyMs) {
        if (response == null) {
            throw new IllegalStateException("Embedding endpoint returned an empty body");
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.size() != expectedCount) {
            throw new IllegalStateException("Embedding endpoint returned "
                    + (data == null ? 0 : data.size()) + " vectors, expected " + expectedCount);
        }
        int expectedDim = properties.getEmbeddingDimension();
        List<float[]> vectors = new ArrayList<>(data.size());
        for (Map<String, Object> item : data) {
            List<Number> values = (List<Number>) item.get("embedding");
            if (values == null) {
                throw new IllegalStateException("Embedding entry missing 'embedding' field");
            }
            if (values.size() != expectedDim) {
                throw new IllegalStateException("Embedding dimension mismatch: model returned "
                        + values.size() + " floats but hospital.ai.embedding-dimension is "
                        + expectedDim + ". Add a new Flyway migration to widen the VECTOR(N) "
                        + "and update embedding-dimension before retrying.");
            }
            float[] vec = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vec[i] = values.get(i).floatValue();
            }
            vectors.add(vec);
        }

        int tokensIn = 0;
        int tokensOut = 0;
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> u) {
            tokensIn = intValue(u.get("prompt_tokens"));
            tokensOut = intValue(u.get("completion_tokens"));
        }
        return new EmbeddingBatchResult(vectors, tokensIn, tokensOut, latencyMs);
    }

    // --- audit helpers (mirror AiAuditService shape so the table stays uniform)

    private void recordSuccess(Long userId, List<String> texts, EmbeddingBatchResult result) {
        AiAuditLog row = baseRow(userId);
        row.setStatus("success");
        row.setPromptExcerpt(truncate(joinExcerpt(texts)));
        row.setResponseExcerpt("[" + result.vectors().size() + " vectors x "
                + properties.getEmbeddingDimension() + " dims]");
        row.setTokensIn(result.tokensIn());
        row.setTokensOut(result.tokensOut());
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, result.latencyMs()));
        safeSave(row);
    }

    private void recordFailure(Long userId, List<String> texts, long latencyMs, Throwable error) {
        AiAuditLog row = baseRow(userId);
        row.setStatus("failed");
        row.setPromptExcerpt(truncate(joinExcerpt(texts)));
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latencyMs));
        row.setErrorMsg(truncate(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        safeSave(row);
    }

    private void recordRateLimited(Long userId, List<String> texts, String reason) {
        AiAuditLog row = baseRow(userId);
        row.setStatus("rate_limited");
        row.setPromptExcerpt(truncate(joinExcerpt(texts)));
        row.setLatencyMs(0);
        row.setErrorMsg(truncate("rate_limit: " + reason));
        safeSave(row);
    }

    private AiAuditLog baseRow(Long userId) {
        AiAuditLog row = new AiAuditLog();
        row.setUserId(userId);
        row.setFeature(FEATURE);
        row.setModel(properties.getEmbeddingModel());
        row.setTraceId(TraceContext.getTraceId());
        return row;
    }

    private void safeSave(AiAuditLog row) {
        try {
            auditRepository.save(row);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist ai_audit_log row for embedding: {}", ex.toString());
        }
    }

    // --- utilities ---------------------------------------------------------

    private static String joinExcerpt(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(t);
            if (sb.length() > 500) break;
        }
        return sb.toString();
    }

    private static int totalChars(List<String> texts) {
        int n = 0;
        for (String t : texts) if (t != null) n += t.length();
        return n;
    }

    private static int intValue(Object n) {
        return n instanceof Number num ? num.intValue() : 0;
    }

    private static boolean isRetryable(HttpStatusCode status) {
        int code = status.value();
        return code == 429 || code >= 500;
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }

    private static void sleep(Duration d) {
        try { Thread.sleep(d.toMillis()); }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiRateLimitException("interrupted", "Backoff interrupted");
        }
    }
}
