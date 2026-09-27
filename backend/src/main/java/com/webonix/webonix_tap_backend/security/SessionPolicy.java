package com.webonix.webonix_tap_backend.security;
import java.time.Duration;
public final class SessionPolicy {
    private SessionPolicy() {}
    public static Duration lifetime(boolean rememberMe) { return rememberMe ? Duration.ofDays(7) : Duration.ofHours(1); }
}
