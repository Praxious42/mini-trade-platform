package com.pbkour.mintrade.authorisation.services;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "MyJwtSecretKey12345MyJwtSecretKey12345MyJwtSecretKey12345";
    private static final long EXPIRATION = 3600000L;

    @Test
    void generate_and_validate_token() throws Exception {
        JwtService jwtService = new JwtService(SECRET, EXPIRATION);

        // Inject private fields that would normally come from @Value in a Spring context
        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, SECRET);

        Field expField = JwtService.class.getDeclaredField("expiration");
        expField.setAccessible(true);
        expField.setLong(jwtService, EXPIRATION);

        String token = jwtService.generateToken("alice");

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");

        boolean valid = jwtService.validateToken(token, "alice");
        assertThat(valid).isTrue();
    }
}

