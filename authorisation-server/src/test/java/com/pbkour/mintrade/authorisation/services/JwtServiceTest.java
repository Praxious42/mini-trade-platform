package com.pbkour.mintrade.authorisation.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {"jwt.secret=MyJwtSecretKey12345MyJwtSecretKey12345MyJwtSecretKey12345", "jwt.expiration=3600000"})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generate_and_validate_token() {
        String token = jwtService.generateToken("alice");

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");

        boolean valid = jwtService.validateToken(token, "alice");
        assertThat(valid).isTrue();
    }
}

