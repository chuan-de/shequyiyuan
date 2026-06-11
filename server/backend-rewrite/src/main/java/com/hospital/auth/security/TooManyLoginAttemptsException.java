package com.hospital.auth.security;

/** 登录失败次数超限。retryAfterSeconds 用于 Retry-After 响应头。 */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("登录尝试次数过多，请 " + Math.max(retryAfterSeconds / 60, 1) + " 分钟后再试");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
