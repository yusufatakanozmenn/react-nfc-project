package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.dto.ChangePasswordRequest;
import com.webonix.webonix_tap_backend.repository.*;
import com.webonix.webonix_tap_backend.security.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordChangeService {
    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordResetRepository resets;
    private final PasswordEncoder encoder;

    public PasswordChangeService(AppUserRepository users, AuthSessionRepository sessions,
            PasswordResetRepository resets, PasswordEncoder encoder) {
        this.users = users; this.sessions = sessions; this.resets = resets; this.encoder = encoder;
    }

    @Transactional
    public void change(String userId, ChangePasswordRequest request) {
        // Same lock as login and recovery: an old password cannot establish a session after this commits.
        var user = users.findByIdForUpdate(Long.valueOf(userId))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String current = request.currentPassword();
        if (current == null || current.isBlank() || current.getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(current, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mevcut şifre hatalı.");
        }
        PasswordPolicy.validate(request.newPassword());
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni şifreler eşleşmiyor.");
        }
        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni şifre mevcut şifrenizden farklı olmalıdır.");
        }
        user.setPassword(encoder.encode(request.newPassword()));
        users.saveAndFlush(user);
        resets.revokeForUser(user.getId());
        sessions.revokeAll(user.getId());
    }
}
