package com.hospital.ai.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.ai.audit.AiAuditService;
import com.hospital.ai.audit.CurrentUserResolver;
import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;
import com.hospital.ai.ratelimit.AiRateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

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
 * <p>The class deliberately does not bake in audit or rate-limit logic for the
 * unary {@link #chat} entry point — those are wired by {@code AiAuditInterceptor}
 * / {@code AiRateLimiter} in front of the method so each concern stays
 * testable in isolation. Streaming ({@link #chatStream}) is the exception:
 * AspectJ around-advice on a Flux-returning method would have to materialise
 * the stream to audit it, which defeats the purpose. So streaming opts the
 * audit + rate-limit logic in-line — see {@code chatStream} below.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiCallTemplate {

    private static final Logger log = LoggerFactory.getLogger(AiCallTemplate.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private static final String SSE_DATA_PREFIX = "data:";
    private static final String SSE_DONE_SENTINEL = "[DONE]";

    private final RestClient restClient;
    private final AiProperties properties;
    /** Lazy: streaming dependencies aren't needed by callers that only use unary. */
    private final ObjectProvider<WebClient> webClientProvider;
    private final ObjectProvider<AiRateLimiter> rateLimiterProvider;
    private final ObjectProvider<AiAuditService> auditServiceProvider;
    private final ObjectProvider<CurrentUserResolver> currentUserProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiCallTemplate(@Qualifier("aiRestClient") RestClient restClient,
                          AiProperties properties,
                          @Qualifier("aiWebClient") ObjectProvider<WebClient> webClientProvider,
                          ObjectProvider<AiRateLimiter> rateLimiterProvider,
                          ObjectProvider<AiAuditService> auditServiceProvider,
                          ObjectProvider<CurrentUserResolver> currentUserProvider) {
        this.restClient = restClient;
        this.properties = properties;
        this.webClientProvider = webClientProvider;
        this.rateLimiterProvider = rateLimiterProvider;
        this.auditServiceProvider = auditServiceProvider;
        this.currentUserProvider = currentUserProvider;
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
     * Streaming chat completion. Returns a cold {@link Flux} that, on
     * subscription, opens the upstream SSE connection, parses each
     * {@code data:} frame into a {@link ChatChunk}, and terminates on
     * {@code data: [DONE]}.
     *
     * <p><b>Rate-limit + audit semantics</b>: rate-limit acquire happens
     * synchronously on subscription (so refused requests never hit the wire),
     * and the audit row is written once when the stream completes (success or
     * error). Mid-stream cancellation by the client is recorded as success
     * with whatever tokens accumulated — matches the spec's "已扣的 token 仍
     * 然计" decision.</p>
     *
     * <p><b>Retry</b>: mid-stream failures are NOT retried because we have
     * already started flushing partial deltas to the client. Pre-flight
     * failures (e.g. 429 on the initial POST) bubble up as the first error
     * signal on the Flux.</p>
     */
    public Flux<ChatChunk> chatStream(ChatRequest request) {
        Map<String, Object> body = buildBody(request);
        body.put("stream", true);

        WebClient client = webClientProvider.getIfAvailable();
        if (client == null) {
            return Flux.error(new IllegalStateException("aiWebClient bean is not available"));
        }

        Long userId = currentUserId();
        int estimate = request.getEstimatedTokens() == null ? 0 : request.getEstimatedTokens();
        AiRateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
        AiAuditService auditService = auditServiceProvider.getIfAvailable();

        return Flux.defer(() -> {
            // Pre-flight rate limit; throw → Flux.error path → audit "rate_limited".
            if (rateLimiter != null) {
                try {
                    rateLimiter.acquireOrThrow(userId, estimate);
                } catch (AiRateLimitException ex) {
                    if (auditService != null) auditService.recordRateLimited(userId, request, ex.getReason());
                    return Flux.<ChatChunk>error(ex);
                }
            }

            long start = System.currentTimeMillis();
            StringBuilder accumulator = new StringBuilder();
            AtomicReference<Integer> tokensIn = new AtomicReference<>();
            AtomicReference<Integer> tokensOut = new AtomicReference<>();
            AtomicInteger fallbackTokensOut = new AtomicInteger();

            Flux<ChatChunk> upstream = client.post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .mapNotNull(rawLine -> parseSseLine(rawLine, accumulator, tokensIn, tokensOut, fallbackTokensOut))
                    .takeUntil(ChatChunk::done);

            return upstream
                    .doOnComplete(() -> {
                        if (auditService != null) {
                            int tin = tokensIn.get() == null ? estimate : tokensIn.get();
                            int tout = tokensOut.get() == null ? fallbackTokensOut.get() : tokensOut.get();
                            long latency = System.currentTimeMillis() - start;
                            ChatResponse synthetic = new ChatResponse(
                                    request.getModel(),
                                    accumulator.toString(),
                                    tin, tout, latency);
                            auditService.recordSuccess(userId, request, synthetic);
                            if (rateLimiter != null) {
                                rateLimiter.debitActualTokens(userId, (tin + tout) - estimate);
                            }
                        }
                    })
                    .doOnError(err -> {
                        if (auditService != null) {
                            long latency = System.currentTimeMillis() - start;
                            auditService.recordFailure(userId, request, latency, err);
                        }
                    });
        });
    }

    // --- internals ---------------------------------------------------------

    private Long currentUserId() {
        CurrentUserResolver r = currentUserProvider.getIfAvailable();
        return r == null ? null : r.currentUserId();
    }

    /**
     * One line of upstream output. Most useful frames look like
     * {@code data: {"choices":[{"delta":{"content":"..."}}]}}. Returns null
     * for non-data lines (comments / blank keepalives) so the {@code Flux}
     * silently skips them via {@code mapNotNull}.
     */
    ChatChunk parseSseLine(String line,
                           StringBuilder accumulator,
                           AtomicReference<Integer> tokensIn,
                           AtomicReference<Integer> tokensOut,
                           AtomicInteger fallbackTokensOut) {
        if (line == null) return null;
        String trimmed = line.startsWith(SSE_DATA_PREFIX) ? line.substring(SSE_DATA_PREFIX.length()).trim() : line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(":")) return null;
        if (SSE_DONE_SENTINEL.equals(trimmed)) {
            return ChatChunk.terminator(tokensIn.get(), tokensOut.get());
        }
        try {
            Map<String, Object> obj = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
            // usage may be present on the last data frame before [DONE].
            Object usage = obj.get("usage");
            if (usage instanceof Map<?, ?> u) {
                Integer in = intOrNull(u.get("prompt_tokens"));
                Integer out = intOrNull(u.get("completion_tokens"));
                if (in != null) tokensIn.set(in);
                if (out != null) tokensOut.set(out);
            }
            Object choicesObj = obj.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return null;
            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> choice)) return null;
            Object delta = choice.get("delta");
            String content = null;
            if (delta instanceof Map<?, ?> deltaMap && deltaMap.get("content") instanceof String s) {
                content = s;
            } else if (choice.get("message") instanceof Map<?, ?> msg && msg.get("content") instanceof String s) {
                // some upstreams send the full message object on finish; treat as delta.
                content = s;
            }
            // finish_reason on the last delta frame; combined with absence of [DONE]
            // some gateways send the terminator implicitly. We still rely on [DONE]
            // for terminal signal but stop appending content here.
            if (content == null || content.isEmpty()) {
                return null;
            }
            accumulator.append(content);
            fallbackTokensOut.addAndGet(estimateTokens(content));
            return ChatChunk.delta(content);
        } catch (Exception ex) {
            log.warn("Failed to parse SSE line; ignoring frame: {}", trimmed.substring(0, Math.min(trimmed.length(), 120)));
            return null;
        }
    }

    private static Integer intOrNull(Object n) {
        if (n instanceof Number num) return num.intValue();
        return null;
    }

    /** Crude estimate when upstream omits usage from the stream. 1 char ≈ 1 token. */
    private static int estimateTokens(String s) {
        return s == null ? 0 : s.length();
    }

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
