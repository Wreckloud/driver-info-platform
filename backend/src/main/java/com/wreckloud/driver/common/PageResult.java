package com.wreckloud.driver.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 分页响应。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "分页响应")
public record PageResult<T>(long total, int page, int pageSize, List<T> items, Instant serverTime) {
}
