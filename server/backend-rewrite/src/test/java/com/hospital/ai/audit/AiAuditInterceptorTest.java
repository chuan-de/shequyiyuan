package com.hospital.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;
import com.hospital.ai.ratelimit.AiRateLimiter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

/**
 * Uses hand-rolled test doubles rather than Mockito because Mockito's inline
 * mock-maker on JDK 25 cannot redefine non-final concrete services in this
 * project. Doubles make the wiring + side-effects explicit anyway.
 */
class AiAuditInterceptorTest {

    private final ChatRequest sampleRequest = ChatRequest.builder()
            .feature("consult")
            .model("doubao-seed-2.0-lite")
            .messages(List.of(ChatMessage.text("user", "hello")))
            .estimatedTokens(50)
            .build();

    @Test
    void onSuccess_debitsActualTokensAndRecordsAuditRow() throws Throwable {
        RecordingAuditService audit = new RecordingAuditService();
        TrackingRateLimiter limiter = new TrackingRateLimiter();
        ChatResponse resp = new ChatResponse("doubao-seed-2.0-lite", "hi", 30, 40, 100);

        AiAuditInterceptor interceptor = new AiAuditInterceptor(audit, limiter, fixedUser(42L));
        Object out = interceptor.around(jp(sampleRequest, resp));

        assertThat(out).isSameAs(resp);
        assertThat(limiter.acquired).containsExactly(new long[] { 42L, 50L });
        // 70 actual − 50 estimate = 20 debited after the fact.
        assertThat(limiter.debited).containsExactly(new long[] { 42L, 20L });
        assertThat(audit.successCalls).hasSize(1);
        assertThat(audit.failureCalls).isEmpty();
    }

    @Test
    void onUpstreamFailure_recordsFailureAndRethrows() {
        RecordingAuditService audit = new RecordingAuditService();
        TrackingRateLimiter limiter = new TrackingRateLimiter();
        RuntimeException boom = new RuntimeException("boom");

        AiAuditInterceptor interceptor = new AiAuditInterceptor(audit, limiter, fixedUser(42L));

        assertThatThrownBy(() -> interceptor.around(jpThrowing(sampleRequest, boom)))
                .isSameAs(boom);
        assertThat(audit.failureCalls).hasSize(1);
        assertThat(audit.failureCalls.get(0).error).isSameAs(boom);
        assertThat(audit.successCalls).isEmpty();
    }

    @Test
    void onRateLimit_skipsUpstreamAndAuditsRateLimited() {
        RecordingAuditService audit = new RecordingAuditService();
        TrackingRateLimiter limiter = new TrackingRateLimiter();
        limiter.throwOnAcquire = new AiRateLimitException("qpm", "rate exceeded");

        AtomicReference<Boolean> proceeded = new AtomicReference<>(false);
        AiAuditInterceptor interceptor = new AiAuditInterceptor(audit, limiter, fixedUser(42L));

        assertThatThrownBy(() -> interceptor.around(new RecordingJoinPoint(sampleRequest, null, null) {
            @Override public Object proceed() { proceeded.set(true); return null; }
        })).isInstanceOf(AiRateLimitException.class);

        assertThat(proceeded.get()).isFalse();
        assertThat(audit.rateLimitedCalls).hasSize(1);
        assertThat(audit.rateLimitedCalls.get(0).reason).isEqualTo("qpm");
    }

    @Test
    void anonymousCaller_passesNullUserIdThrough() throws Throwable {
        RecordingAuditService audit = new RecordingAuditService();
        TrackingRateLimiter limiter = new TrackingRateLimiter();
        ChatResponse resp = new ChatResponse("m", "x", 1, 1, 1);

        AiAuditInterceptor interceptor = new AiAuditInterceptor(audit, limiter, fixedUser(null));
        interceptor.around(jp(sampleRequest, resp));

        assertThat(audit.successCalls).hasSize(1);
        assertThat(audit.successCalls.get(0).userId).isNull();
    }

    // --- helpers -----------------------------------------------------------

    private static CurrentUserResolver fixedUser(Long id) {
        return new CurrentUserResolver(null) {
            @Override public Long currentUserId() { return id; }
            @Override public String currentUsername() { return id == null ? null : "u" + id; }
        };
    }

    private static ProceedingJoinPoint jp(ChatRequest req, ChatResponse resp) {
        return new RecordingJoinPoint(req, resp, null);
    }

    private static ProceedingJoinPoint jpThrowing(ChatRequest req, Throwable t) {
        return new RecordingJoinPoint(req, null, t);
    }

    private static class RecordingJoinPoint implements ProceedingJoinPoint {
        private final Object arg;
        private final Object result;
        private final Throwable error;

        RecordingJoinPoint(Object arg, Object result, Throwable error) {
            this.arg = arg; this.result = result; this.error = error;
        }

        @Override public Object proceed() throws Throwable {
            if (error != null) throw error;
            return result;
        }
        @Override public Object proceed(Object[] args) throws Throwable { return proceed(); }
        @Override public Object[] getArgs() { return new Object[] { arg }; }
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure a) {}
        @Override public Signature getSignature() { return null; }
        @Override public org.aspectj.lang.reflect.SourceLocation getSourceLocation() { return null; }
        @Override public String getKind() { return "method-execution"; }
        @Override public Object getThis() { return null; }
        @Override public Object getTarget() { return null; }
        @Override public org.aspectj.lang.JoinPoint.StaticPart getStaticPart() { return null; }
        @Override public String toShortString() { return "jp"; }
        @Override public String toLongString() { return "jp"; }
    }

    /** AiAuditService subclass that records calls instead of hitting JPA. */
    static class RecordingAuditService extends AiAuditService {
        record Success(Long userId, ChatRequest req, ChatResponse resp) {}
        record Failure(Long userId, ChatRequest req, long latency, Throwable error) {}
        record RateLimited(Long userId, ChatRequest req, String reason) {}
        final List<Success> successCalls = new ArrayList<>();
        final List<Failure> failureCalls = new ArrayList<>();
        final List<RateLimited> rateLimitedCalls = new ArrayList<>();

        RecordingAuditService() { super(null); }

        @Override public AiAuditLog recordSuccess(Long userId, ChatRequest req, ChatResponse resp) {
            successCalls.add(new Success(userId, req, resp));
            return new AiAuditLog();
        }
        @Override public AiAuditLog recordFailure(Long userId, ChatRequest req, long latency, Throwable error) {
            failureCalls.add(new Failure(userId, req, latency, error));
            return new AiAuditLog();
        }
        @Override public AiAuditLog recordRateLimited(Long userId, ChatRequest req, String reason) {
            rateLimitedCalls.add(new RateLimited(userId, req, reason));
            return new AiAuditLog();
        }
    }

    /** AiRateLimiter subclass that records acquire/debit pairs. */
    static class TrackingRateLimiter extends AiRateLimiter {
        final List<long[]> acquired = new ArrayList<>();
        final List<long[]> debited = new ArrayList<>();
        AiRateLimitException throwOnAcquire;

        TrackingRateLimiter() { super(defaultProps()); }

        @Override public void acquireOrThrow(Long userId, int estimatedTokens) {
            acquired.add(new long[] { userId == null ? -1L : userId, estimatedTokens });
            if (throwOnAcquire != null) throw throwOnAcquire;
        }
        @Override public void debitActualTokens(Long userId, int additionalTokens) {
            if (additionalTokens <= 0) return;
            debited.add(new long[] { userId == null ? -1L : userId, additionalTokens });
        }
        private static AiProperties defaultProps() {
            AiProperties p = new AiProperties();
            p.getRateLimit().setPerUserQpm(1000);
            p.getRateLimit().setDailyTokenBudget(1_000_000L);
            return p;
        }
    }
}
