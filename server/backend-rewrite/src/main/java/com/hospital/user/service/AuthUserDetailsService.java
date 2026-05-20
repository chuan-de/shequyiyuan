package com.hospital.user.service;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbcClient;

    public AuthUserDetailsService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUserRow userRow = jdbcClient.sql("""
                SELECT id, username, password_hash, enabled
                FROM app_user
                WHERE username = :username
            """)
            .param("username", username)
            .query(AuthUserRow.class)
            .optional()
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = jdbcClient.sql("""
                SELECT authority_code
                FROM (
                    SELECT CONCAT('ROLE_', r.role_code) AS authority_code
                    FROM app_user_role ur
                    JOIN app_role r ON ur.role_id = r.id
                    WHERE ur.user_id = :userId
                    UNION
                    SELECT p.permission_code AS authority_code
                    FROM app_user_role ur
                    JOIN app_role_permission rp ON ur.role_id = rp.role_id
                    JOIN app_permission p ON rp.permission_id = p.id
                    WHERE ur.user_id = :userId
                ) t
            """)
            .param("userId", userRow.id())
            .query(String.class)
            .list()
            .stream()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();

        return User.withUsername(userRow.username())
            .password(userRow.passwordHash())
            .authorities(authorities)
            .disabled(!userRow.enabled())
            .build();
    }

    public record AuthUserRow(Long id, String username, String passwordHash, Boolean enabled) {
    }
}
