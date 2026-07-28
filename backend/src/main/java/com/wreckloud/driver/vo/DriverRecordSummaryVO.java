package com.wreckloud.driver.vo;

import com.wreckloud.driver.domain.LocationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 司机提交成功摘要。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "司机提交成功摘要")
public record DriverRecordSummaryVO(
        Long id,
        String driverName,
        String licensePlate,
        String destination,
        LocationStatus locationStatus,
        BigDecimal latitude,
        BigDecimal longitude,
        String locationAddress,
        BigDecimal locationAccuracy,
        Instant createdAt) {
}
