package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.security.*;
import com.webonix.webonix_tap_backend.config.ProductionSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityHardeningTests {
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    @Test void accountLimitNormalizesEmailAndSpansAddressesThenExpires() {
        var clock = new MutableClock(); var limiter = new LoginRateLimiter(clock);
        for (int i = 0; i < 10; i++) assertEquals(0, limiter.retryAfter("ip" + i, " User@Example.test "));
        assertEquals(900, limiter.retryAfter("another", "user@example.test"));
        clock.now = clock.now.plusSeconds(900);
        assertEquals(0, limiter.retryAfter("another", "user@example.test"));
    }
    @Test void ipLimitSpansAccounts() {
        var limiter = new LoginRateLimiter(new MutableClock());
        for (int i = 0; i < 30; i++) assertEquals(0, limiter.retryAfter("one", "user" + i));
        assertEquals(900, limiter.retryAfter("one", "new-account"));
        assertEquals(0, limiter.retryAfter("two", "new-account"));
    }
    @Test void concurrentAttemptsCannotBypassAccountLimit() {
        var limiter = new LoginRateLimiter(new MutableClock());
        long allowed = java.util.stream.IntStream.range(0, 100).parallel()
                .filter(i -> limiter.retryAfter("ip" + i, "same@example.test") == 0).count();
        assertEquals(10, allowed);
    }
    @Test void saturatedLimiterFailsClosedAndRecovers() {
        var clock = new MutableClock(); var limiter = new LoginRateLimiter(clock);
        for (int i = 0; i < 4999; i++) assertEquals(0, limiter.retryAfter("ip" + i, "user" + i));
        assertTrue(limiter.retryAfter("new-ip", "new-user") > 0);
        clock.now = clock.now.plusSeconds(900);
        assertEquals(0, limiter.retryAfter("new-ip", "new-user"));
    }
    @Test void productionCookiesUseHostPrefixSecureHttpOnlyAndStrict() {
        var cookies = new AuthCookies(true); var response = new MockHttpServletResponse();
        cookies.write(response, "test-only");
        String header = response.getHeader("Set-Cookie");
        assertTrue(header.startsWith("__Host-webonix_session="));
        assertTrue(header.contains("Secure")); assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Strict")); assertFalse(header.contains("Domain="));
        assertTrue(header.contains("Max-Age=3600"));
    }
    MockEnvironment production() {
        return new MockEnvironment().withProperty("app.cookies.secure", "true")
                .withProperty("app.cors.allowed-origins", "https://panel.example.test")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.datasource.username", "webonix_app");
    }
    @Test void productionRejectsInsecureCookiesOriginsSchemaAndRoot() {
        assertDoesNotThrow(() -> new ProductionSecurityConfig(production()));
        assertThrows(IllegalStateException.class, () -> new ProductionSecurityConfig(production().withProperty("app.cookies.secure", "false")));
        assertThrows(IllegalStateException.class, () -> new ProductionSecurityConfig(production().withProperty("spring.jpa.hibernate.ddl-auto", "update")));
        assertThrows(IllegalStateException.class, () -> new ProductionSecurityConfig(production().withProperty("spring.datasource.username", "root")));
        for (String origin : new String[]{"http://panel.example.test", "*", "https://panel.example.test/path", "https://panel.example.test#bad"}) {
            assertThrows(IllegalStateException.class, () -> new ProductionSecurityConfig(production().withProperty("app.cors.allowed-origins", origin)));
        }
    }
}
