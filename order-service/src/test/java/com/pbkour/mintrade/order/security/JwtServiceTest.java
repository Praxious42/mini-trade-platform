package com.pbkour.mintrade.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {"security.jwt.secret=short-secret-for-test", "security.jwt.expiration=3600000"})
@Disabled("Disabled while resource-server wiring is in progress")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void extractUsername_fromValidToken() throws Exception {
        String secret = "short-secret-for-test";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
        Key key = Keys.hmacShaKeyFor(hash);

        String token = Jwts.builder()
            .setSubject("alice")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice");
    }
}
