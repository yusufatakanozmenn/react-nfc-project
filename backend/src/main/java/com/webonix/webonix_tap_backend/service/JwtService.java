package com.webonix.webonix_tap_backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;


    public JwtService(@org.springframework.beans.factory.annotation.Value("${app.jwt.secret}") String secret) {
        // Keys validates the minimum HMAC key length; there is no shared fallback secret.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(
            Long userId,
            String role
    ) {
        return generateToken(userId, role, false);
    }

    public String generateToken(Long userId, String role, boolean rememberMe) {
        Date now = new Date();

        Date expiration =
                new Date(now.getTime() + com.webonix.webonix_tap_backend.security.SessionPolicy.lifetime(rememberMe).toMillis());

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
