package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.ChangePasswordRequest;
import com.webonix.webonix_tap_backend.service.PasswordChangeService;
import com.webonix.webonix_tap_backend.security.*;
import jakarta.servlet.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/me/password")
public class PasswordChangeController {
    private final PasswordChangeService service;
    private final AuthCookies cookies;
    private final LoginRateLimiter attempts = new LoginRateLimiter();

    public PasswordChangeController(PasswordChangeService service, AuthCookies cookies) {
        this.service = service; this.cookies = cookies;
    }

    @PutMapping
    public ResponseEntity<?> change(Principal principal, @RequestBody ChangePasswordRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        long retry = attempts.retryAfter(request.getRemoteAddr(), principal.getName());
        if (retry > 0) return ResponseEntity.status(429).header("Retry-After", Long.toString(retry))
                .body(Map.of("message", "Çok fazla şifre değiştirme denemesi. Lütfen daha sonra tekrar deneyin."));
        // Account identity comes exclusively from the authenticated principal.
        service.change(principal.getName(), body);
        cookies.write(response, null);
        return ResponseEntity.noContent().build();
    }
}
