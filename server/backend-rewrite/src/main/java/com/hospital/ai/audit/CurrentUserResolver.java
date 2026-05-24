package com.hospital.ai.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves the numeric {@code app_user.id} for the currently authenticated
 * principal. {@link com.hospital.user.service.AuthUserDetailsService} only
 * exposes the username on the {@link UserDetails}, so this resolver does a
 * cheap JDBC lookup to map username → id for audit / rate-limit purposes.
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class CurrentUserResolver {

    private final JdbcClient jdbcClient;

    public CurrentUserResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** @return the current user's database id, or {@code null} when anonymous. */
    public Long currentUserId() {
        String username = currentUsername();
        if (username == null) return null;
        return jdbcClient.sql("SELECT id FROM app_user WHERE username = :u")
                .param("u", username)
                .query(Long.class)
                .optional()
                .orElse(null);
    }

    /** @return the current principal's username, or {@code null} when anonymous. */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails u) return u.getUsername();
        if (principal instanceof String s && !"anonymousUser".equals(s)) return s;
        return null;
    }
}
