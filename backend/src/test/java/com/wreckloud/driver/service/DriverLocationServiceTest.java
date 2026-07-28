package com.wreckloud.driver.service;

import com.wreckloud.driver.dto.LocationAddressRequest;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.security.LocationResolveRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 司机端定位地址解析服务测试。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@ExtendWith(MockitoExtension.class)
class DriverLocationServiceTest {
    private static final BigDecimal LATITUDE = new BigDecimal("31.342193");
    private static final BigDecimal LONGITUDE = new BigDecimal("120.656864");

    @Mock
    private ReverseGeocodingService reverseGeocodingService;

    private DriverLocationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-28T02:00:00Z"), ZoneOffset.UTC);
        service = new DriverLocationService(reverseGeocodingService, new LocationResolveRateLimiter(clock));
    }

    @Test
    void shouldReturnResolvedAddress() {
        when(reverseGeocodingService.resolveAddress(LATITUDE, LONGITUDE))
                .thenReturn("江苏省苏州市张家港市");

        var result = service.resolveAddress(new LocationAddressRequest(LATITUDE, LONGITUDE), "127.0.0.1");

        assertThat(result.address()).isEqualTo("江苏省苏州市张家港市");
    }

    @Test
    void shouldRejectRequestsBeyondRateLimit() {
        LocationAddressRequest request = new LocationAddressRequest(LATITUDE, LONGITUDE);
        for (int index = 0; index < 20; index++) {
            service.resolveAddress(request, "127.0.0.1");
        }

        assertThatThrownBy(() -> service.resolveAddress(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("定位请求过于频繁，请稍后重试");
    }
}
