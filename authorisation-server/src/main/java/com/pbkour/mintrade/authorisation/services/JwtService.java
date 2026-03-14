package com.pbkour.mintrade.authorisation.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusMillis(expiration)))
            .signWith(SignatureAlgorithm.HS256, secret) // TODO: Use a stronger algorithm and manage keys securely in production
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration()
            .before(Date.from(Instant.now()));
    }

    public boolean validateToken(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }
}
