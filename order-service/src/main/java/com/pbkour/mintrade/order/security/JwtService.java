package com.pbkour.mintrade.order.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    @Value("${security.jwt.secret:${jwt.secret:change_me_dev_secret}}")
    private String secret;

    @Value("${security.jwt.expiration:3600000}")
    private long expiration;

    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public Date extractExpiration(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();
    }

    public Object extractClaim(String token, String name) {
        return Jwts.parser()
            .setSigningKey(secret)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get(name);
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp != null && exp.before(Date.from(Instant.now()));
    }

    public boolean validateToken(String token, String username) {
        try {
            return extractUsername(token).equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
