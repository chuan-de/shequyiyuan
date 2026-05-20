package com.hospital.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties properties = new JwtProperties("01234567890123456789012345678901", Duration.ofHours(1));
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.generateToken("alice", "USER");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals("USER", jwtService.extractRole(token));
    }
}
