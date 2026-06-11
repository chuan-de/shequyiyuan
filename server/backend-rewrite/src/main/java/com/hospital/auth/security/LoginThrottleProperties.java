package com.hospital.auth.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security.login-throttle")
public record LoginThrottleProperties(
    @DefaultValue("5") int maxAttempts,
    @DefaultValue("15m") Duration lockDuration) {
}
