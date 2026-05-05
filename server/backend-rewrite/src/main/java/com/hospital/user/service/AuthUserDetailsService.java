package com.hospital.user.service;

import com.hospital.auth.entity.AppUser;
import com.hospital.auth.repository.AppUserRepository;
import com.hospital.auth.repository.AppUserRoleRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final AppUserRoleRepository appUserRoleRepository;

    public AuthUserDetailsService(AppUserRepository appUserRepository, AppUserRoleRepository appUserRoleRepository) {
        this.appUserRepository = appUserRepository;
        this.appUserRoleRepository = appUserRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = appUserRoleRepository.findByIdUserId(user.getId())
            .stream()
            .map(link -> new SimpleGrantedAuthority("ROLE_" + link.getRole().getRoleCode()))
            .map(GrantedAuthority.class::cast)
            .toList();

        return User.withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .authorities(authorities)
            .disabled(!user.getEnabled())
            .build();
    }
}
