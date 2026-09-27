package com.webonix.webonix_tap_backend.security;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Single-instance, bounded memory limiter. Never trust client supplied forwarding headers. */
@Component
public class LoginRateLimiter {
    private static final long WINDOW = 15 * 60 * 1000L;
    private static final int MAX_KEYS = 10000;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    private final int accountLimit, ipLimit;
    public LoginRateLimiter() { this(Clock.systemUTC()); }
    public LoginRateLimiter(Clock clock) { this(clock, 10, 30); }
    public LoginRateLimiter(Clock clock, int accountLimit, int ipLimit) { this.clock = clock; this.accountLimit = accountLimit; this.ipLimit = ipLimit; }
    private record Bucket(long expires, int count) {}
    public synchronized long retryAfter(String address, String email) {
        long now = clock.millis();
        buckets.entrySet().removeIf(e -> e.getValue().expires <= now);
        String ip = "ip:" + address;
        String account = "account:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        if (account.length() > 160) account = "account:invalid";
        long wait = Math.max(waitFor(ip, ipLimit, now), waitFor(account, accountLimit, now));
        if (wait > 0) return wait;
        // Fail closed under saturation rather than evicting limits an attacker can bypass.
        if (buckets.size() >= MAX_KEYS - 2) return 60;
        increment(ip, now); increment(account, now);
        return 0;
    }
    private long waitFor(String key, int limit, long now) {
        Bucket b = buckets.get(key);
        return b != null && b.count >= limit ? Math.max(1, (b.expires - now + 999) / 1000) : 0;
    }
    private void increment(String key, long now) {
        buckets.compute(key, (k, b) -> b == null ? new Bucket(now + WINDOW, 1) : new Bucket(b.expires, b.count + 1));
    }
}
