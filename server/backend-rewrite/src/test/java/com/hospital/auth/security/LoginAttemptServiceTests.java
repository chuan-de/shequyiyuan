package com.hospital.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** 纯单测（无 Spring 上下文），用可拨动的 Clock 验证锁定与过期。 */
class LoginAttemptServiceTests {

    private static final LoginThrottleProperties PROPS =
        new LoginThrottleProperties(3, Duration.ofMinutes(15));

    /** 可手动前拨的 Clock。 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-06-11T00:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @Test
    void belowThreshold_shouldNotBlock() {
        LoginAttemptService service = new LoginAttemptService(PROPS, new MutableClock());
        service.recordFailure("alice", "1.2.3.4");
        service.recordFailure("alice", "1.2.3.4");
        assertEquals(0, service.blockedForSeconds("alice", "1.2.3.4"));
    }

    @Test
    void reachingThreshold_shouldBlockForLockDuration() {
        LoginAttemptService service = new LoginAttemptService(PROPS, new MutableClock());
        for (int i = 0; i < 3; i++) service.recordFailure("alice", "1.2.3.4");

        long blocked = service.blockedForSeconds("alice", "1.2.3.4");
        assertTrue(blocked > 0 && blocked <= Duration.ofMinutes(15).toSeconds());

        // 其他用户 / 其他 IP 不受影响
        assertEquals(0, service.blockedForSeconds("bob", "1.2.3.4"));
        assertEquals(0, service.blockedForSeconds("alice", "5.6.7.8"));
    }

    @Test
    void lock_shouldExpireAfterLockDuration() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(PROPS, clock);
        for (int i = 0; i < 3; i++) service.recordFailure("alice", "1.2.3.4");
        assertTrue(service.blockedForSeconds("alice", "1.2.3.4") > 0);

        clock.advance(Duration.ofMinutes(16));
        assertEquals(0, service.blockedForSeconds("alice", "1.2.3.4"));
    }

    @Test
    void successfulLogin_shouldResetCounter() {
        LoginAttemptService service = new LoginAttemptService(PROPS, new MutableClock());
        service.recordFailure("alice", "1.2.3.4");
        service.recordFailure("alice", "1.2.3.4");
        service.recordSuccess("alice", "1.2.3.4");
        // 清零后重新累计，再失败两次仍未达阈值
        service.recordFailure("alice", "1.2.3.4");
        service.recordFailure("alice", "1.2.3.4");
        assertEquals(0, service.blockedForSeconds("alice", "1.2.3.4"));
    }

    @Test
    void staleFailures_shouldStartFreshWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(PROPS, clock);
        service.recordFailure("alice", "1.2.3.4");
        service.recordFailure("alice", "1.2.3.4");

        // 上次失败已超过锁定时长，计数应重置而非累加
        clock.advance(Duration.ofMinutes(16));
        service.recordFailure("alice", "1.2.3.4");
        assertEquals(0, service.blockedForSeconds("alice", "1.2.3.4"));
    }
}
