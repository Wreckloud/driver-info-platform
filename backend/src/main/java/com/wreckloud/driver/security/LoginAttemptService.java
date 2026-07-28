package com.wreckloud.driver.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单实例管理员登录失败限流。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String clientAddress) {
        Attempt attempt = attempts.get(clientAddress);
        if (attempt == null || attempt.lockedUntil() == null) {
            return false;
        }
        if (!clock.instant().isBefore(attempt.lockedUntil())) {
            attempts.remove(clientAddress, attempt);
            return false;
        }
        return true;
    }

    public void recordFailure(String clientAddress) {
        attempts.compute(clientAddress, (key, current) -> {
            Instant now = clock.instant();
            boolean outsideFailureWindow = current == null
                    || Duration.between(current.lastFailure(), now).compareTo(LOCK_DURATION) >= 0;
            int failures = outsideFailureWindow ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempt(failures, now, lockedUntil);
        });
    }

    public void recordSuccess(String clientAddress) {
        attempts.remove(clientAddress);
    }

    private record Attempt(int failures, Instant lastFailure, Instant lockedUntil) {
    }
}
