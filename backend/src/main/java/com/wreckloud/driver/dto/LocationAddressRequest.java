package com.wreckloud.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 定位文字地址解析参数。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@Schema(description = "定位文字地址解析参数")
public record LocationAddressRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        @Schema(description = "WGS84 纬度") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        @Schema(description = "WGS84 经度") BigDecimal longitude) {
}
