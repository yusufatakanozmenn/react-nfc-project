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
@Import({com.webonix.webonix_tap_backend.service.SessionService.class, com.webonix.webonix_tap_backend.security.AuthCookies.class, SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class, JwtService.class, AuthService.class})
class WebonixTapBackendApplicationTests extends SecurityTestSupport {
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
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(cards.getAllCards("1")).thenReturn(List.of());
    }

    private String bearer() { return "Bearer " + sessions.issue(1L, "USER"); }

    @Test void successfulLoginThenMeAndProtectedCards() throws Exception {
        String request = mapper.writeValueAsString(java.util.Map.of("email", EMAIL.toUpperCase(), "password", PASSWORD));
        var response = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse();
        String token = response.getCookie("webonix_session").getValue();
        org.junit.jupiter.api.Assertions.assertTrue(response.getCookie("webonix_session").isHttpOnly());
        org.junit.jupiter.api.Assertions.assertTrue(response.getHeader("Set-Cookie").contains("SameSite=Strict"));
        mvc.perform(get("/api/auth/me").cookie(cookie("Bearer " + token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Database Name"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/api/cards").cookie(cookie("Bearer " + token))).andExpect(status().isOk());
    }

    @Test void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cards")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cards").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/cards/1").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/cards/1/status").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/cards/1").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test void malformedTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(cookie("Bearer invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test void expiredTokenIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject("1").expiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me").cookie(cookie("Bearer " + token)))
                .andExpect(status().isUnauthorized());
    }

    @Test void differentSigningKeyIsUnauthorized() throws Exception {
        var other = new JwtService(UUID.randomUUID().toString().repeat(2));
        mvc.perform(get("/api/auth/me").cookie(cookie("Bearer " + other.generateToken(1L, "ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test void tokenWithoutExpirationIsUnauthorized() throws Exception {
        String token = Jwts.builder().subject("1")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me").cookie(cookie("Bearer " + token)))
                .andExpect(status().isUnauthorized());
    }

    @Test void disabledUserCannotUsePreviouslyIssuedToken() throws Exception {
        String token = bearer();
        user.setActive(false);
        mvc.perform(get("/api/auth/me").cookie(cookie(token))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cards").cookie(cookie(token))).andExpect(status().isUnauthorized());
    }

    @Test void deletedUserCannotUsePreviouslyIssuedToken() throws Exception {
        String token = bearer();
        when(users.findById(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").cookie(cookie(token))).andExpect(status().isUnauthorized());
    }

    @Test void currentIdentityComesFromDatabase() throws Exception {
        String token = bearer();
        user.setName("Updated Name");
        user.setRole("ADMIN");
        mvc.perform(get("/api/auth/me").cookie(cookie(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void missingLoginFieldsAreBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void corsAllowsFrontendWithAuthorization() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-xsrf-token"))
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

    @Test void loginLogoutAndWritesRejectMissingCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        String token = bearer();
        mvc.perform(post("/api/auth/logout").cookie(cookie(token))).andExpect(status().isForbidden());
        mvc.perform(put("/api/auth/me").cookie(cookie(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").cookie(cookie(token))).andExpect(status().isOk());
    }
    @Test void forgedCsrfAndForeignOriginAreRejected() throws Exception {
        mvc.perform(post("/api/auth/logout").cookie(cookie(bearer())).with(csrf()).header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(cookie(bearer()), new jakarta.servlet.http.Cookie("webonix_csrf", "one"))
                .header("X-XSRF-TOKEN", "two")).andExpect(status().isForbidden());
    }
    @Test void logoutRevokesCopiedCookieAndIsIdempotent() throws Exception {
        String token = bearer();
        mvc.perform(post("/api/auth/logout").cookie(cookie(token)).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie().maxAge("webonix_session", 0));
        mvc.perform(get("/api/auth/me").cookie(cookie(token))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").cookie(cookie(token)).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void signedButUnregisteredTokenAndLegacyBearerAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(cookie(jwt.generateToken(1L, "USER")))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer())).andExpect(status().isUnauthorized());
    }
    @Test void publicRegistrationIsClosedForGuestUserAndAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").with(csrf()).cookie(cookie(bearer()))).andExpect(status().isForbidden());
        user.setRole("ADMIN");
        mvc.perform(post("/api/auth/register").with(csrf()).cookie(cookie(bearer()))).andExpect(status().isForbidden());
    }
    @Test void repeatedLoginReplacesExistingSession() throws Exception {
        String old = bearer();
        var result = mvc.perform(post("/api/auth/login").with(csrf()).cookie(cookie(old))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse();
        mvc.perform(get("/api/auth/me").cookie(cookie(old))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(result.getCookie("webonix_session"))).andExpect(status().isOk());
    }
    @Test void limiterReturns429AndRetryAfterBeforePasswordLookup() throws Exception {
        when(limiter.retryAfter(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(120L);
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "120"));
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }
    @Test void disabledUnknownAndWrongPasswordHaveSamePublicError() throws Exception {
        String wrong = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email", EMAIL, "password", "wrong"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        user.setActive(false);
        String disabled = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email", "missing@example.test", "password", PASSWORD))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(wrong, disabled);
        org.junit.jupiter.api.Assertions.assertEquals(wrong, unknown);
    }
    @Test void csrfResponseIsNotCacheableAndCookieIsHttpOnly() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie().httpOnly("webonix_csrf", true))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
    @Test void csrfTokenWithoutItsCookieCannotAuthorizeMutation() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        String csrf = mapper.readTree(result.getContentAsString()).get("token").asString();
        mvc.perform(post("/api/auth/logout").header("X-XSRF-TOKEN", csrf).cookie(cookie(bearer())))
                .andExpect(status().isForbidden());
    }
}
