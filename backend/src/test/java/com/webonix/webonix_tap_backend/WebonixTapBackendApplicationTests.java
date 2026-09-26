package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.config.SecurityConfig;
import com.webonix.webonix_tap_backend.config.WebConfig;
import com.webonix.webonix_tap_backend.controller.AuthController;
import com.webonix.webonix_tap_backend.controller.NfcCardController;
import com.webonix.webonix_tap_backend.entity.AppUser;
import com.webonix.webonix_tap_backend.repository.AppUserRepository;
import com.webonix.webonix_tap_backend.security.JwtAuthenticationFilter;
import com.webonix.webonix_tap_backend.service.AuthService;
import com.webonix.webonix_tap_backend.service.JwtService;
import com.webonix.webonix_tap_backend.service.NfcCardService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real controllers, BCrypt, JWT and security chain; repositories are mocked, so no MySQL writes.
@WebMvcTest({AuthController.class, NfcCardController.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class, JwtService.class, AuthService.class})
class WebonixTapBackendApplicationTests {
    private static final String SECRET = UUID.randomUUID().toString().repeat(2);
    private static final String EMAIL = "auth-test@example.test";
    private static final String PASSWORD = UUID.randomUUID().toString();
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @MockitoBean AppUserRepository users;
    @MockitoBean NfcCardService cards;
    private AppUser user;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRET);
    }

    @BeforeEach
    void setup() {
        user = new AppUser("Database Name", EMAIL, encoder.encode(PASSWORD), "USER", true);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cards.getAllCards()).thenReturn(List.of());
    }

    private String bearer() { return "Bearer " + jwt.generateToken(EMAIL, "USER"); }

    @Test void successfulLoginThenMeAndProtectedCards() throws Exception {
        String request = mapper.writeValueAsString(java.util.Map.of("email", EMAIL.toUpperCase(), "password", PASSWORD));
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("token").asString();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Database Name"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/api/cards").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cards")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cards")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/cards/1")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/cards/1/status")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/cards/1")).andExpect(status().isUnauthorized());
    }

    @Test void malformedTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test void expiredTokenIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject(EMAIL).expiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void differentSigningKeyIsUnauthorized() throws Exception {
        var other = new JwtService(UUID.randomUUID().toString().repeat(2));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + other.generateToken(EMAIL, "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test void tokenWithoutExpirationIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject(EMAIL)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void disabledUserCannotUsePreviouslyIssuedToken() throws Exception {
        String token = bearer();
        user.setActive(false);
        mvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cards").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test void deletedUserCannotUsePreviouslyIssuedToken() throws Exception {
        String token = bearer();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test void currentIdentityComesFromDatabase() throws Exception {
        String token = bearer();
        user.setName("Updated Name");
        user.setRole("ADMIN");
        mvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void missingLoginFieldsAreBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void corsAllowsFrontendWithAuthorization() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test void corsRejectsUnconfiguredOrigin() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test void unknownPublicRedirectIs404RatherThanAuthenticationError() throws Exception {
        mvc.perform(get("/r/not-implemented")).andExpect(status().isNotFound());
    }
}
