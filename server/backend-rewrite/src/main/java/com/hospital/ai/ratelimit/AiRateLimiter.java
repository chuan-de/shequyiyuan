package com.hospital.ai.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Per-user AI rate limiting via Bucket4j. Two independent buckets are kept:
 *
 * <ul>
 *   <li><b>QPM bucket</b> — caps requests per minute (configured by
 *       {@code hospital.ai.rate-limit.per-user-qpm}).</li>
 *   <li><b>Daily token bucket</b> — caps total tokens (in + out) per 24h
 *       (configured by {@code hospital.ai.rate-limit.daily-token-budget}).</li>
 * </ul>
 *
 * <p>Buckets are held in an in-memory {@link ConcurrentHashMap}. That's fine
 * for the current single-instance deployment; if we ever scale out we'll swap
 * in a Redis-backed Bucket4j proxy.</p>
 *
 * <p>Callers go through {@link #acquireOrThrow(Long, int)} before the upstream
 * call, then {@link #debitActualTokens(Long, int)} after the response so the
 * daily bucket reflects real usage, not just the pre-call estimate.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiRateLimiter {

    /** Used when no authenticated user is available (e.g. internal warmup). */
    static final Long ANONYMOUS_BUCKET = -1L;

    private final AiProperties.RateLimit config;
    private final ConcurrentMap<Long, Bucket> qpmBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Bucket> dailyTokenBuckets = new ConcurrentHashMap<>();

    public AiRateLimiter(AiProperties properties) {
        this.config = properties.getRateLimit();
    }

    /**
     * Reserve quota for one upcoming call. Throws {@link AiRateLimitException}
     * if either bucket is exhausted.
     *
     * @param estimatedTokens caller's best guess for total tokens this call
     *                         will consume; 0 is acceptable but discouraged
     *                         because the daily bucket then only debits after
     *                         the fact.
     */
    public void acquireOrThrow(Long userId, int estimatedTokens) {
        Long key = userId == null ? ANONYMOUS_BUCKET : userId;

        if (!qpmBucket(key).tryConsume(1)) {
            throw new AiRateLimitException("qpm",
                    "AI request rate limit exceeded (" + config.getPerUserQpm() + " per minute)");
        }
        if (estimatedTokens > 0 && !dailyTokenBucket(key).tryConsume(estimatedTokens)) {
            throw new AiRateLimitException("daily-token-budget",
                    "AI daily token budget exhausted (" + config.getDailyTokenBudget() + " tokens)");
        }
    }

    /**
     * Convenience for callers outside the interceptor that want a boolean
     * instead of an exception.
     */
    public boolean tryAcquire(Long userId, int estimatedTokens) {
        try {
            acquireOrThrow(userId, estimatedTokens);
            return true;
        } catch (AiRateLimitException ignored) {
            return false;
        }
    }

    /**
     * Debit the actual additional tokens consumed after the upstream returned,
     * relative to the pre-call estimate. Negative deltas are ignored (we don't
     * refund — keeps the bucket monotonic).
     */
    public void debitActualTokens(Long userId, int additionalTokens) {
        if (additionalTokens <= 0) return;
        Long key = userId == null ? ANONYMOUS_BUCKET : userId;
        dailyTokenBucket(key).tryConsume(additionalTokens);
    }

    private Bucket qpmBucket(Long key) {
        return qpmBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(config.getPerUserQpm())
                        .refillIntervally(config.getPerUserQpm(), Duration.ofMinutes(1))
                        .build())
                .build());
    }

    private Bucket dailyTokenBucket(Long key) {
        return dailyTokenBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(config.getDailyTokenBudget())
                        .refillIntervally(config.getDailyTokenBudget(), Duration.ofDays(1))
                        .build())
                .build());
    }
}
