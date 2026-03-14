package com.pbkour.mintrade.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {"security.jwt.secret=MyJwtSecretKey12345MyJwtSecretKey12345MyJwtSecretKey12345", "security.jwt.expiration=3600000"})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void extractUsername_fromValidToken() {
        String secret = "MyJwtSecretKey12345MyJwtSecretKey12345MyJwtSecretKey12345";

        String token = Jwts.builder()
            .setSubject("alice")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");
    }
}
