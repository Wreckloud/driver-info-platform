package com.wreckloud.driver.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单实例定位地址解析限流器。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@Service
public class LocationResolveRateLimiter {
    private static final int MAX_REQUESTS = 20;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public LocationResolveRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String clientAddress) {
        Instant now = clock.instant();
        AtomicBoolean allowed = new AtomicBoolean();
        windows.compute(clientAddress, (key, current) -> {
            if (current == null || Duration.between(current.startedAt(), now).compareTo(WINDOW_DURATION) >= 0) {
                allowed.set(true);
                return new RequestWindow(now, 1);
            }
            if (current.requestCount() >= MAX_REQUESTS) {
                return current;
            }
            allowed.set(true);
            return new RequestWindow(current.startedAt(), current.requestCount() + 1);
        });
        return allowed.get();
    }

    private record RequestWindow(Instant startedAt, int requestCount) {
    }
}
