package com.webonix.webonix_tap_backend.security;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class AuthCookies {
    private final boolean secure;
    public AuthCookies(@Value("${app.cookies.secure:false}") boolean secure) { this.secure = secure; }
    public String name() { return secure ? "__Host-webonix_session" : "webonix_session"; }
    public String read(HttpServletRequest request) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name())) return cookie.getValue();
        }
        return null;
    }
    public void write(HttpServletResponse response, String token) {
        write(response, token, false);
    }
    public void write(HttpServletResponse response, String token, boolean rememberMe) {
        var cookie = ResponseCookie.from(name(), token == null ? "" : token)
                .httpOnly(true).secure(secure).sameSite("Strict").path("/");
        if (token == null) cookie.maxAge(Duration.ZERO);
        else if (rememberMe) cookie.maxAge(SessionPolicy.lifetime(true));
        response.addHeader("Set-Cookie", cookie.build().toString());
    }
}
