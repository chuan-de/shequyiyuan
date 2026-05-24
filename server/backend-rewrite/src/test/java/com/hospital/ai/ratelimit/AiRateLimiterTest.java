package com.hospital.ai.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;

import org.junit.jupiter.api.Test;

class AiRateLimiterTest {

    private AiRateLimiter limiterWith(int qpm, long dailyBudget) {
        AiProperties props = new AiProperties();
        props.getRateLimit().setPerUserQpm(qpm);
        props.getRateLimit().setDailyTokenBudget(dailyBudget);
        return new AiRateLimiter(props);
    }

    @Test
    void qpmBucket_blocksOncePerUserExceedsConfiguredRate() {
        AiRateLimiter limiter = limiterWith(3, 1_000_000L);

        // First 3 succeed.
        assertThat(limiter.tryAcquire(1L, 0)).isTrue();
        assertThat(limiter.tryAcquire(1L, 0)).isTrue();
        assertThat(limiter.tryAcquire(1L, 0)).isTrue();
        // 4th in the same minute is rejected.
        assertThatThrownBy(() -> limiter.acquireOrThrow(1L, 0))
                .isInstanceOf(AiRateLimitException.class)
                .satisfies(ex -> assertThat(((AiRateLimitException) ex).getReason()).isEqualTo("qpm"));
    }

    @Test
    void qpmBucket_isIndependentPerUser() {
        AiRateLimiter limiter = limiterWith(1, 1_000_000L);

        assertThat(limiter.tryAcquire(1L, 0)).isTrue();
        assertThat(limiter.tryAcquire(1L, 0)).isFalse();
        // Different user still has full capacity.
        assertThat(limiter.tryAcquire(2L, 0)).isTrue();
    }

    @Test
    void dailyTokenBudget_blocksWhenEstimateExceedsRemaining() {
        AiRateLimiter limiter = limiterWith(1000, 100L);

        // First call estimates 60 tokens — OK.
        assertThat(limiter.tryAcquire(1L, 60)).isTrue();
        // Second call estimates 50 tokens — only 40 left, must reject.
        assertThatThrownBy(() -> limiter.acquireOrThrow(1L, 50))
                .isInstanceOf(AiRateLimitException.class)
                .satisfies(ex -> assertThat(((AiRateLimitException) ex).getReason()).isEqualTo("daily-token-budget"));
    }

    @Test
    void debitActualTokens_reducesRemainingBudget() {
        AiRateLimiter limiter = limiterWith(1000, 100L);

        // Estimate 30, actual 80 → debit 50 after the fact.
        limiter.acquireOrThrow(7L, 30);
        limiter.debitActualTokens(7L, 50);
        // Now 20 left. Next 30-token call should fail.
        assertThat(limiter.tryAcquire(7L, 30)).isFalse();
        assertThat(limiter.tryAcquire(7L, 20)).isTrue();
    }

    @Test
    void anonymousUser_isBucketed() {
        AiRateLimiter limiter = limiterWith(1, 1_000_000L);
        assertThat(limiter.tryAcquire(null, 0)).isTrue();
        assertThat(limiter.tryAcquire(null, 0)).isFalse();
    }
}
