package com.wreckloud.driver.service;

import java.math.BigDecimal;

/**
 * 经纬度逆地址解析服务。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public interface ReverseGeocodingService {

    String resolveAddress(BigDecimal latitude, BigDecimal longitude);
}
