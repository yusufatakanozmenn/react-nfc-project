package com.webonix.webonix_tap_backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions", indexes = @Index(name = "idx_auth_session_expiry", columnList = "expires_at"))
public class AuthSession {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Instant expiresAt;
    protected AuthSession() {}
    public AuthSession(String id, Long userId, Instant expiresAt) {
        this.id = id; this.userId = userId; this.expiresAt = expiresAt;
    }
    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
