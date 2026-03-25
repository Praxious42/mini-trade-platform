package com.pbkour.mintrade.authorisation.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final String secret = "MyJwtSecretKey12345MyJwtSecretKey12345MyJwtSecretKey12345";
    private final long expiration = 3600000L; // 1 hour in ms
    private final JwtService jwtService = new JwtService(secret, expiration);

    @Test
    void generate_and_validate_token() {
        String token = jwtService.generateToken("alice");

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");

        boolean valid = jwtService.validateToken(token, "alice");
        assertThat(valid).isTrue();
    }
}

