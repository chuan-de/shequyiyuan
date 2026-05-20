package com.hospital.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
