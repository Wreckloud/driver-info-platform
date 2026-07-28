package com.wreckloud.driver.service;

import com.wreckloud.driver.dto.LocationAddressRequest;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.security.LocationResolveRateLimiter;
import com.wreckloud.driver.vo.LocationAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 司机端定位地址解析服务。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@Service
@RequiredArgsConstructor
public class DriverLocationService {
    private final ReverseGeocodingService reverseGeocodingService;
    private final LocationResolveRateLimiter rateLimiter;

    public LocationAddressVO resolveAddress(LocationAddressRequest request, String clientAddress) {
        if (!rateLimiter.tryAcquire(clientAddress)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "定位请求过于频繁，请稍后重试");
        }
        String address = reverseGeocodingService.resolveAddress(request.latitude(), request.longitude());
        return new LocationAddressVO(address);
    }
}
