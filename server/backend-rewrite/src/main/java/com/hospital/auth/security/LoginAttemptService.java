package com.hospital.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 登录失败限流（内存实现，防口令爆破）。
 *
 * 计数键为 username|ip 组合：同一来源对同一账号连续失败 maxAttempts 次后，
 * 锁定 lockDuration；登录成功即清零。单实例部署内存计数足够；
 * 若将来横向扩展，需换 Redis 等共享存储。
 */
@Service
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final LoginThrottleProperties props;
    private final Clock clock;
    private final ConcurrentMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(LoginThrottleProperties props) {
        this(props, Clock.systemUTC());
    }

    LoginAttemptService(LoginThrottleProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    /** 已被锁定时返回剩余秒数，否则返回 0。 */
    public long blockedForSeconds(String username, String clientIp) {
        Attempt attempt = attempts.get(key(username, clientIp));
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long remaining = attempt.lockedUntil.getEpochSecond() - clock.instant().getEpochSecond();
        if (remaining <= 0) {
            attempts.remove(key(username, clientIp));
            return 0;
        }
        return remaining;
    }

    public void recordFailure(String username, String clientIp) {
        Instant now = clock.instant();
        attempts.compute(key(username, clientIp), (k, attempt) -> {
            // 距上次失败超过锁定时长则视为新一轮计数
            if (attempt == null || attempt.lastFailure.plus(props.lockDuration()).isBefore(now)) {
                attempt = new Attempt();
            }
            attempt.count++;
            attempt.lastFailure = now;
            if (attempt.count >= props.maxAttempts()) {
                attempt.lockedUntil = now.plus(props.lockDuration());
            }
            return attempt;
        });
        if (attempts.size() > CLEANUP_THRESHOLD) {
            purgeExpired(now);
        }
    }

    public void recordSuccess(String username, String clientIp) {
        attempts.remove(key(username, clientIp));
    }

    private void purgeExpired(Instant now) {
        attempts.entrySet().removeIf(e -> e.getValue().lastFailure.plus(props.lockDuration()).isBefore(now));
    }

    private String key(String username, String clientIp) {
        return username.toLowerCase(Locale.ROOT) + "|" + clientIp;
    }

    private static final class Attempt {
        int count;
        Instant lastFailure = Instant.EPOCH;
        Instant lockedUntil;
    }
}
