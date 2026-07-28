package com.wreckloud.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 定位文字地址解析结果。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@Schema(description = "定位文字地址解析结果")
public record LocationAddressVO(
        @Schema(description = "文字地址；解析失败时为空") String address) {
}
