package com.webonix.webonix_tap_backend.service;
import com.webonix.webonix_tap_backend.entity.PasswordReset;
import com.webonix.webonix_tap_backend.repository.*;
import com.webonix.webonix_tap_backend.security.PasswordPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class PasswordResetService {
    private final AppUserRepository users;
    private final PasswordResetRepository resets;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final PasswordResetMailer mailer;
    private final String frontend;
    private final SecureRandom random = new SecureRandom();
    public PasswordResetService(AppUserRepository users, PasswordResetRepository resets, AuthSessionRepository sessions,
            PasswordEncoder encoder, PasswordResetMailer mailer,
            @Value("${app.frontend-url:http://localhost:5173}") String frontend,
            @Value("${app.cookies.secure:false}") boolean secure) {
        this.users=users; this.resets=resets; this.sessions=sessions; this.encoder=encoder; this.mailer=mailer;
        URI uri = URI.create(frontend);
        boolean local = !secure && "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
        if ((!local && !"https".equals(uri.getScheme())) || uri.getHost()==null || uri.getUserInfo()!=null
                || uri.getQuery()!=null || uri.getFragment()!=null || !uri.getPath().isEmpty())
            throw new IllegalStateException("Frontend URL must be a trusted HTTPS origin (localhost HTTP is allowed in development).");
        this.frontend=frontend;
    }
    @Transactional
    public void request(String email) {
        var user = users.findByEmailForUpdate(email).filter(u -> Boolean.TRUE.equals(u.getActive())).orElse(null);
        if (user == null) return;
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        resets.saveAndFlush(new PasswordReset(user.getId(), hash(token), user.getEmail(), Instant.now().plusSeconds(900)));
        // SMTP failure rolls back the new token, preserving any previous valid link.
        mailer.sendLink(email, frontend + "/reset-password#token=" + token);
    }
    @Transactional
    public void reset(String token, String password) {
        PasswordPolicy.validate(password);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String hash=hash(token);
        var entry=resets.findByTokenHash(hash).orElseThrow(PasswordResetService::invalid);
        var user=users.findByIdForUpdate(entry.getUserId()).filter(u -> Boolean.TRUE.equals(u.getActive())).orElseThrow(PasswordResetService::invalid);
        if (!entry.getEmail().equals(user.getEmail()) || resets.consume(user.getId(), hash, Instant.now()) != 1) throw invalid();
        user.setPassword(encoder.encode(password)); users.saveAndFlush(user);
        sessions.revokeAll(user.getId());
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bağlantı geçersiz veya süresi dolmuş. Yeni bir sıfırlama bağlantısı isteyin.");
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
