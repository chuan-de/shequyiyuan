package com.hospital.auth.service;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.CurrentUserResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.security.JwtProperties;
import com.hospital.auth.security.JwtService;
import com.hospital.user.service.AuthUserDetailsService;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUserDetailsService authUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final JdbcClient jdbcClient;

    public AuthService(
        AuthenticationManager authenticationManager,
        AuthUserDetailsService authUserDetailsService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        JwtProperties jwtProperties,
        JdbcClient jdbcClient
    ) {
        this.authenticationManager = authenticationManager;
        this.authUserDetailsService = authUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.jdbcClient = jdbcClient;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = authUserDetailsService.loadUserByUsername(request.username());
        String roleCode = userDetails.getAuthorities().stream()
            .findFirst()
            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
            .orElse("USER");

        String token = jwtService.generateToken(request.username(), roleCode);
        return new AuthResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    @Transactional
    public void register(RegisterRequest request) {
        Integer existing = jdbcClient.sql("SELECT COUNT(1) FROM app_user WHERE username = :username")
            .param("username", request.username())
            .query(Integer.class)
            .single();
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("Username already exists");
        }

        Long userId = jdbcClient.sql("""
                INSERT INTO app_user (username, password_hash, enabled)
                VALUES (:username, :passwordHash, true)
                RETURNING id
            """)
            .param("username", request.username())
            .param("passwordHash", passwordEncoder.encode(request.password()))
            .query(Long.class)
            .single();

        Long userRoleId = jdbcClient.sql("SELECT id FROM app_role WHERE role_code = 'USER'")
            .query(Long.class)
            .optional()
            .orElseThrow(() -> new IllegalStateException("Missing USER role seed data"));

        jdbcClient.sql("INSERT INTO app_user_role (user_id, role_id) VALUES (:userId, :roleId)")
            .param("userId", userId)
            .param("roleId", userRoleId)
            .update();
    }

    public CurrentUserResponse currentUser(String username) {
        AuthUserDetailsService.AuthUserRow row = jdbcClient.sql("""
                SELECT id, username, password_hash, enabled
                FROM app_user
                WHERE username = :username
            """)
            .param("username", username)
            .query(AuthUserDetailsService.AuthUserRow.class)
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = jdbcClient.sql("""
                SELECT r.role_code
                FROM app_user_role ur
                JOIN app_role r ON ur.role_id = r.id
                WHERE ur.user_id = :userId
            """)
            .param("userId", row.id())
            .query(String.class)
            .list();

        return new CurrentUserResponse(row.username(), row.enabled(), roles);
    }
}
