package com.wreckloud.driver.vo;

import com.wreckloud.driver.domain.LocationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 管理员登记记录视图。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "管理员登记记录")
public record DriverRecordVO(
        Long id,
        String project,
        String driverName,
        String phone,
        String licensePlate,
        String vehicleType,
        String quantity,
        String destination,
        String remark,
        BigDecimal latitude,
        BigDecimal longitude,
        String locationAddress,
        BigDecimal locationAccuracy,
        LocationStatus locationStatus,
        Instant locatedAt,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy) {
}
