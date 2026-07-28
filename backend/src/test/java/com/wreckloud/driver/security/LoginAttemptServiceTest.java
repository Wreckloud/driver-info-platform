package com.wreckloud.driver.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录失败限流测试。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
class LoginAttemptServiceTest {

    @Test
    void shouldBlockAfterFiveFailuresAndClearAfterSuccess() {
        LoginAttemptService service = new LoginAttemptService(
                Clock.fixed(Instant.parse("2026-07-27T00:00:00Z"), ZoneOffset.UTC));
        String clientAddress = "127.0.0.1";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(clientAddress);
        }
        assertThat(service.isBlocked(clientAddress)).isTrue();

        service.recordSuccess(clientAddress);
        assertThat(service.isBlocked(clientAddress)).isFalse();
    }
}
