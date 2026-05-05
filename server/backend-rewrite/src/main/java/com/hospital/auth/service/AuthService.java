package com.hospital.auth.service;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.CurrentUserResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.entity.AppRole;
import com.hospital.auth.entity.AppUser;
import com.hospital.auth.entity.AppUserRole;
import com.hospital.auth.repository.AppRoleRepository;
import com.hospital.auth.repository.AppRolePermissionRepository;
import com.hospital.auth.repository.AppUserRepository;
import com.hospital.auth.repository.AppUserRoleRepository;
import com.hospital.auth.security.JwtProperties;
import com.hospital.auth.security.JwtService;
import com.hospital.user.service.AuthUserDetailsService;
import java.util.List;
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
    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final AppRolePermissionRepository appRolePermissionRepository;

    public AuthService(
        AuthenticationManager authenticationManager,
        AuthUserDetailsService authUserDetailsService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        JwtProperties jwtProperties,
        AppUserRepository appUserRepository,
        AppRoleRepository appRoleRepository,
        AppUserRoleRepository appUserRoleRepository,
        AppRolePermissionRepository appRolePermissionRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.authUserDetailsService = authUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.appUserRoleRepository = appUserRoleRepository;
        this.appRolePermissionRepository = appRolePermissionRepository;
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
        if (appUserRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        AppUser savedUser = appUserRepository.save(user);

        AppRole userRole = appRoleRepository.findByRoleCode("USER")
            .orElseThrow(() -> new IllegalStateException("Missing USER role seed data"));

        appUserRoleRepository.save(new AppUserRole(savedUser, userRole));
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String username) {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = appUserRoleRepository.findByIdUserId(user.getId())
            .stream()
            .map(link -> link.getRole().getRoleCode())
            .toList();

        List<String> permissions = appUserRoleRepository.findByIdUserId(user.getId())
            .stream()
            .flatMap(link -> appRolePermissionRepository.findByIdRoleId(link.getRole().getId()).stream())
            .map(link -> link.getPermission().getPermissionCode())
            .distinct()
            .toList();

        return new CurrentUserResponse(user.getUsername(), user.getEnabled(), roles, permissions);
    }
}
