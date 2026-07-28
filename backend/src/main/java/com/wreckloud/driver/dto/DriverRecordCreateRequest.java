package com.wreckloud.driver.dto;

import com.wreckloud.driver.domain.LocationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 司机登记创建参数。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "司机登记创建参数")
public record DriverRecordCreateRequest(
        @NotNull @Schema(description = "客户端生成的幂等令牌") UUID submissionToken,
        @NotBlank @Size(max = 50) @Schema(description = "司机姓名") String driverName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "大陆手机号") String phone,
        @NotBlank @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9-]{5,12}$", message = "车牌号格式不正确")
        @Schema(description = "车牌号") String licensePlate,
        @NotBlank @Size(max = 50) @Schema(description = "车型") String vehicleType,
        @NotBlank @Size(max = 200) @Schema(description = "目的地") String destination,
        @NotNull @Schema(description = "定位状态") LocationStatus locationStatus,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
        @DecimalMin(value = "0.01") BigDecimal locationAccuracy,
        @Schema(description = "设备获取定位的时间") Instant locatedAt) {

    @AssertTrue(message = "定位状态与定位数据不一致")
    @Schema(hidden = true)
    public boolean isLocationDataConsistent() {
        if (locationStatus == null) {
            return true;
        }
        boolean hasAllLocationData = latitude != null && longitude != null
                && locationAccuracy != null && locatedAt != null;
        boolean hasAnyLocationData = latitude != null || longitude != null
                || locationAccuracy != null || locatedAt != null;
        return locationStatus == LocationStatus.SUCCESS ? hasAllLocationData : !hasAnyLocationData;
    }
}
