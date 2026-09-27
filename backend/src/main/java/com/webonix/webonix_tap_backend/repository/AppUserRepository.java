package com.webonix.webonix_tap_backend.repository;

import com.webonix.webonix_tap_backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForUpdate(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(Long id);

    boolean existsByEmail(String email);

    java.util.List<AppUser> findByRoleOrderByNameAsc(String role);
}