package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.*;
import com.webonix.webonix_tap_backend.service.*;
import com.webonix.webonix_tap_backend.security.*;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionService sessions;
    private final AuthCookies cookies;
    private final LoginRateLimiter limiter;
    public AuthController(AuthService auth, SessionService sessions, AuthCookies cookies, LoginRateLimiter limiter) {
        this.auth = auth; this.sessions = sessions; this.cookies = cookies; this.limiter = limiter;
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @GetMapping("/me")
    public CurrentUserResponse me(Principal principal) { return auth.currentUser(principal.getName()); }
    @PutMapping("/me")
    public CurrentUserResponse updateMe(Principal principal, @RequestBody UpdateProfileRequest body) {
        return auth.updateProfile(principal.getName(), body);
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        long retry = limiter.retryAfter(request.getRemoteAddr(), body.email());
        if (retry > 0) return ResponseEntity.status(429).header("Retry-After", Long.toString(retry))
                .body(Map.of("message", "Çok fazla giriş denemesi. Lütfen daha sonra tekrar deneyin."));
        AuthResponse result = auth.login(body);
        sessions.revoke(cookies.read(request));
        cookies.write(response, result.token(), Boolean.TRUE.equals(body.rememberMe()));
        return ResponseEntity.ok(new CurrentUserResponse(result.id(), result.name(), result.email(), result.role()));
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.revoke(cookies.read(request));
        cookies.write(response, null);
    }
}
