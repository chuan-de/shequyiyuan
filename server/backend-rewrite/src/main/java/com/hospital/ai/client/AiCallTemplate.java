package com.hospital.ai.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import reactor.core.publisher.Flux;

/**
 * Single seam for all Doubao (Volcengine Ark) OpenAI-compatible calls.
 *
 * <p>Responsibilities centralised here so individual features can't get them
 * wrong:</p>
 * <ul>
 *   <li>POST {@code ${base-url}/chat/completions} with {@code Bearer} auth.</li>
 *   <li>Force {@code temperature = 1} — Doubao 400s on any other value.</li>
 *   <li>{@code timeout-seconds} request timeout (handled by the shared RestClient).</li>
 *   <li>Retry on HTTP 429 and 5xx — 3 attempts, exponential backoff 1s × 2^n.</li>
 *   <li>Return tokens/latency so callers (and the audit interceptor) can debit
 *       budgets and log without re-parsing the upstream response.</li>
 * </ul>
 *
 * <p>The class deliberately does not bake in audit or rate-limit logic — those
 * are wired by {@code AiAuditInterceptor} / {@code AiRateLimiter} in front of
 * the public {@link #chat(ChatRequest)} entry point so each concern stays
 * testable in isolation.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiCallTemplate {

    private static final Logger log = LoggerFactory.getLogger(AiCallTemplate.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final AiProperties properties;

    public AiCallTemplate(@Qualifier("aiRestClient") RestClient restClient, AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Synchronous chat completion. Caller MUST NOT set temperature — this
     * method always forces it to 1.
     */
    public ChatResponse chat(ChatRequest request) {
        return chatWith(request, this::doCall);
    }

    /** Test seam: callers can swap in a mock HTTP function. */
    ChatResponse chatWith(ChatRequest request, Function<Map<String, Object>, RawResponse> caller) {
        Map<String, Object> body = buildBody(request);

        RestClientResponseException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long start = System.currentTimeMillis();
            try {
                RawResponse raw = caller.apply(body);
                long latency = System.currentTimeMillis() - start;
                return parse(raw, request.getModel(), latency);
            } catch (RestClientResponseException ex) {
                lastError = ex;
                HttpStatusCode status = ex.getStatusCode();
                if (!isRetryable(status) || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }
                Duration backoff = BASE_BACKOFF.multipliedBy(1L << (attempt - 1));
                log.warn("AI call attempt {} failed with {} — retrying in {}ms",
                        attempt, status.value(), backoff.toMillis());
                sleep(backoff);
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("unreachable");
    }

    /**
     * Streaming chat completion — reserved for Phase 3 (community AI consult).
     * Signature is in place so feature code can compile against it; the
     * implementation is intentionally a TODO so we don't ship untested SSE.
     */
    public Flux<ChatChunk> chatStream(ChatRequest request) {
        throw new UnsupportedOperationException("chatStream will be implemented in Phase 3");
    }

    // --- internals ---------------------------------------------------------

    Map<String, Object> buildBody(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());
        body.put("temperature", 1); // Doubao requirement — never make this configurable.
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        List<Map<String, Object>> msgs = new ArrayList<>(request.getMessages().size());
        for (ChatMessage m : request.getMessages()) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            msgs.add(msg);
        }
        body.put("messages", msgs);
        return body;
    }

    @SuppressWarnings("unchecked")
    private RawResponse doCall(Map<String, Object> body) {
        Map<String, Object> response = restClient.post()
                .uri(properties.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        return new RawResponse(response);
    }

    @SuppressWarnings("unchecked")
    static ChatResponse parse(RawResponse raw, String model, long latencyMs) {
        Map<String, Object> response = raw.body();
        if (response == null) {
            return new ChatResponse(model, "", 0, 0, latencyMs);
        }
        String content = "";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null && message.get("content") instanceof String s) {
                content = s;
            }
        }
        int tokensIn = 0;
        int tokensOut = 0;
        Object usageObj = response.get("usage");
        if (usageObj instanceof Map<?, ?> usage) {
            tokensIn = intValue(usage.get("prompt_tokens"));
            tokensOut = intValue(usage.get("completion_tokens"));
        }
        String returnedModel = response.get("model") instanceof String s ? s : model;
        return new ChatResponse(returnedModel, content, tokensIn, tokensOut, latencyMs);
    }

    private static int intValue(Object n) {
        if (n instanceof Number num) return num.intValue();
        return 0;
    }

    private static boolean isRetryable(HttpStatusCode status) {
        int code = status.value();
        return code == 429 || code >= 500;
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiRateLimitException("interrupted", "Backoff interrupted");
        }
    }

    /** Internal carrier so test seam can inject canned bodies. */
    public record RawResponse(Map<String, Object> body) { }
}
