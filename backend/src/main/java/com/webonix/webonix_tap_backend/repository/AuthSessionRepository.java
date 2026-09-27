package com.webonix.webonix_tap_backend.repository;

import com.webonix.webonix_tap_backend.entity.AuthSession;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    @Modifying @Query("delete from AuthSession s where s.id = :id")
    void revoke(String id);
    @Modifying @Query("delete from AuthSession s where s.userId = :userId")
    void revokeAll(Long userId);
    @Modifying @Query("delete from AuthSession s where s.expiresAt <= :now")
    void deleteExpired(Instant now);
}
