package com.hospital.auth.dto;

import java.util.List;

public record CurrentUserResponse(
    String username,
    boolean enabled,
    List<String> roles,
    List<String> permissions
) {
}
