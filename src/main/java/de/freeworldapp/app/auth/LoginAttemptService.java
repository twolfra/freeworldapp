package de.freeworldapp.app.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account-based login brute-force protection: 10 failed password attempts
 * lock the account name for 15 minutes (independent of source IP).
 * In-memory and per-instance, like the rate limiter.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 10;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record State(int failures, Instant lockedUntil) {}

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        State s = states.get(key(username));
        if (s == null || s.lockedUntil() == null) return false;
        if (s.lockedUntil().isBefore(Instant.now())) {
            states.remove(key(username));
            return false;
        }
        return true;
    }

    public void onFailure(String username) {
        states.compute(key(username), (k, s) -> {
            int failures = (s == null ? 0 : s.failures()) + 1;
            if (failures >= MAX_FAILURES) {
                return new State(0, Instant.now().plus(LOCK_DURATION));
            }
            return new State(failures, s == null ? null : s.lockedUntil());
        });
    }

    public void onSuccess(String username) {
        states.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
