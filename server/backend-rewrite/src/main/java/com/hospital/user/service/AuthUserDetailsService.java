package com.hospital.user.service;

import com.hospital.auth.entity.AppUser;
import com.hospital.auth.repository.AppRolePermissionRepository;
import com.hospital.auth.repository.AppUserRepository;
import com.hospital.auth.repository.AppUserRoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final AppRolePermissionRepository appRolePermissionRepository;

    public AuthUserDetailsService(AppUserRepository appUserRepository, AppUserRoleRepository appUserRoleRepository, AppRolePermissionRepository appRolePermissionRepository) {
        this.appUserRepository = appUserRepository;
        this.appUserRoleRepository = appUserRoleRepository;
        this.appRolePermissionRepository = appRolePermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var userRoles = appUserRoleRepository.findByIdUserIdWithRole(user.getId());

        List<Long> roleIds = userRoles.stream()
            .map(link -> link.getRole().getId())
            .toList();

        List<GrantedAuthority> roles = userRoles.stream()
            .map(link -> new SimpleGrantedAuthority("ROLE_" + link.getRole().getRoleCode()))
            .map(GrantedAuthority.class::cast)
            .toList();

        List<GrantedAuthority> permissions = roleIds.isEmpty()
            ? List.of()
            : appRolePermissionRepository.findByIdRoleIdInWithPermission(roleIds)
                .stream()
                .map(link -> new SimpleGrantedAuthority(link.getPermission().getPermissionCode()))
                .map(GrantedAuthority.class::cast)
                .toList();

        List<GrantedAuthority> authorities = new java.util.ArrayList<>(new LinkedHashSet<>(roles));
        authorities.addAll(new LinkedHashSet<>(permissions));

        return User.withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .authorities(authorities)
            .disabled(!user.getEnabled())
            .build();
    }
}
