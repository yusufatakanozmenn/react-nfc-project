package com.webonix.webonix_tap_backend.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "password_resets")
public class PasswordReset {
    @Id private Long userId;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false, length = 150) private String email;
    @Column(nullable = false) private Instant expiresAt;
    protected PasswordReset() {}
    public PasswordReset(Long userId, String tokenHash, String email, Instant expiresAt) {
        this.userId = userId; this.tokenHash = tokenHash; this.email = email; this.expiresAt = expiresAt;
    }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public String getEmail() { return email; }
    public Instant getExpiresAt() { return expiresAt; }
}
