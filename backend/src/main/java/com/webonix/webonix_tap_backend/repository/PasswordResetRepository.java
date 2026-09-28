package com.webonix.webonix_tap_backend.repository;
import com.webonix.webonix_tap_backend.entity.PasswordReset;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.Optional;
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    @Modifying @Query("delete from PasswordReset r where r.userId = :userId")
    void revokeForUser(Long userId);
    Optional<PasswordReset> findByTokenHash(String tokenHash);
    @Modifying @Query("delete from PasswordReset r where r.userId = :userId and r.tokenHash = :hash and r.expiresAt > :now")
    int consume(Long userId, String hash, Instant now);
}
