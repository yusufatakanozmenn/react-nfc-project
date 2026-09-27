package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.entity.AuthSession;
import com.webonix.webonix_tap_backend.repository.AuthSessionRepository;
import com.webonix.webonix_tap_backend.service.SessionService;
import com.webonix.webonix_tap_backend.security.LoginRateLimiter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

abstract class SecurityTestSupport {
    @Autowired MockMvc securityMvc;
    @Autowired ObjectMapper securityMapper;
    @Autowired SessionService sessions;
    @MockitoBean AuthSessionRepository sessionRepository;
    @MockitoBean LoginRateLimiter limiter;
    @BeforeEach void sessionStore() {
        var store = new ConcurrentHashMap<String, AuthSession>();
        when(sessionRepository.save(any())).thenAnswer(call -> {
            AuthSession session = call.getArgument(0); store.put(session.getId(), session); return session;
        });
        when(sessionRepository.findById(anyString())).thenAnswer(call -> Optional.ofNullable(store.get(call.getArgument(0))));
        doAnswer(call -> { store.remove(call.getArgument(0)); return null; }).when(sessionRepository).revoke(anyString());
    }
    Cookie cookie(String value) { return new Cookie("webonix_session", value.replaceFirst("^Bearer ", "")); }
    RequestPostProcessor csrf() throws Exception {
        var response = securityMvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        String token = securityMapper.readTree(response.getContentAsString()).get("token").asString();
        Cookie csrfCookie = response.getCookie("webonix_csrf");
        return request -> {
            Cookie[] existing = request.getCookies();
            var cookies = new java.util.ArrayList<Cookie>();
            if (existing != null) cookies.addAll(java.util.List.of(existing));
            cookies.add(csrfCookie); request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", token); return request;
        };
    }
}
