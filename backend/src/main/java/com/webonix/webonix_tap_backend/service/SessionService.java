package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.entity.AuthSession;
import com.webonix.webonix_tap_backend.repository.AuthSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class SessionService {
    private final AuthSessionRepository sessions;
    private final JwtService jwt;
    public SessionService(AuthSessionRepository sessions, JwtService jwt) {
        this.sessions = sessions; this.jwt = jwt;
    }
    @Transactional
    public String issue(Long userId, String role) {
        return issue(userId, role, false);
    }
    @Transactional
    public String issue(Long userId, String role, boolean rememberMe) {
        String token = jwt.generateToken(userId, role, rememberMe);
        sessions.deleteExpired(Instant.now());
        sessions.save(new AuthSession(hash(token), userId, jwt.getClaims(token).getExpiration().toInstant()));
        return token;
    }
    public boolean isActive(String token, Long userId) {
        return sessions.findById(hash(token))
                .filter(s -> s.getUserId().equals(userId) && s.getExpiresAt().isAfter(Instant.now())).isPresent();
    }
    @Transactional
    public void revoke(String token) {
        if (token != null) sessions.revoke(hash(token));
    }
    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
