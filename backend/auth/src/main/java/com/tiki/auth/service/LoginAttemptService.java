package com.tiki.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ BUG 28 FIX: Simple in-memory brute-force protection for login endpoint.
 *
 * Tracks failed login attempts per username/IP.
 * After MAX_ATTEMPTS failures within WINDOW_MS, the account is temporarily locked.
 *
 * NOTE: For production, this should be backed by Redis to support horizontal scaling.
 * This in-memory version works correctly for a single-instance deployment.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes
    private static final long LOCKOUT_MS = 15 * 60 * 1000L; // 15 minutes lockout

    // key: username or IP → [failCount, windowStartMs]
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    /**
     * Call on successful login — reset attempt counter.
     */
    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    /**
     * Call on failed login — increment counter.
     */
    public void loginFailed(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, current) -> {
            if (current == null || now - current[1] > WINDOW_MS) {
                return new long[]{1, now};
            }
            current[0]++;
            return current;
        });

        long[] window = attempts.get(key);
        if (window != null && window[0] >= MAX_ATTEMPTS) {
            log.warn("SECURITY: Account/IP '{}' locked after {} failed login attempts", key, window[0]);
        }
    }

    /**
     * Returns true if the key is currently locked out.
     */
    public boolean isBlocked(String key) {
        long[] window = attempts.get(key);
        if (window == null) return false;

        long now = System.currentTimeMillis();
        // Reset window if lockout period has passed
        if (now - window[1] > LOCKOUT_MS) {
            attempts.remove(key);
            return false;
        }

        return window[0] >= MAX_ATTEMPTS;
    }

    /**
     * Returns remaining lockout time in seconds, or 0 if not locked.
     */
    public long getRemainingLockoutSeconds(String key) {
        long[] window = attempts.get(key);
        if (window == null || window[0] < MAX_ATTEMPTS) return 0;
        long elapsed = System.currentTimeMillis() - window[1];
        long remaining = (LOCKOUT_MS - elapsed) / 1000;
        return Math.max(0, remaining);
    }
}
