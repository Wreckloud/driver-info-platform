package com.wreckloud.driver.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一接口响应。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "统一接口响应")
public record ApiResult<T>(
        @Schema(description = "1 成功，0 失败") int code,
        @Schema(description = "响应消息") String msg,
        @Schema(description = "响应数据") T data) {

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(1, "success", data);
    }

    public static ApiResult<Void> success() {
        return new ApiResult<>(1, "success", null);
    }

    public static ApiResult<Void> error(String message) {
        return new ApiResult<>(0, message, null);
    }
}
