package com.hospital.ai.audit;

import com.hospital.ai.client.AiCallTemplate;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.ratelimit.AiRateLimiter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Wraps every call to {@link AiCallTemplate#chat(ChatRequest)} with:
 * <ol>
 *   <li>Rate-limit pre-check ({@link AiRateLimiter}).</li>
 *   <li>Audit write on success / failure / rate-limited.</li>
 * </ol>
 *
 * <p>Using an AspectJ {@code @Around} keeps {@code AiCallTemplate} free of
 * cross-cutting noise — the template stays a pure HTTP client that's trivial
 * to unit-test.</p>
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiAuditInterceptor {

    private final AiAuditService auditService;
    private final AiRateLimiter rateLimiter;
    private final CurrentUserResolver currentUserResolver;

    public AiAuditInterceptor(AiAuditService auditService,
                              AiRateLimiter rateLimiter,
                              CurrentUserResolver currentUserResolver) {
        this.auditService = auditService;
        this.rateLimiter = rateLimiter;
        this.currentUserResolver = currentUserResolver;
    }

    @Around("execution(* com.hospital.ai.client.AiCallTemplate.chat(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (args.length == 0 || !(args[0] instanceof ChatRequest request)) {
            return pjp.proceed();
        }

        Long userId = currentUserResolver.currentUserId();
        int estimate = request.getEstimatedTokens() == null ? 0 : request.getEstimatedTokens();

        try {
            rateLimiter.acquireOrThrow(userId, estimate);
        } catch (AiRateLimitException ex) {
            auditService.recordRateLimited(userId, request, ex.getReason());
            throw ex;
        }

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            if (result instanceof ChatResponse response) {
                rateLimiter.debitActualTokens(userId, response.totalTokens() - estimate);
                auditService.recordSuccess(userId, request, response);
            }
            return result;
        } catch (Throwable t) {
            long latency = System.currentTimeMillis() - start;
            auditService.recordFailure(userId, request, latency, t);
            throw t;
        }
    }
}
