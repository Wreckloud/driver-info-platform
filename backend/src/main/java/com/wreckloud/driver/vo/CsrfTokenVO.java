package com.wreckloud.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CSRF 令牌信息。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "CSRF 令牌信息")
public record CsrfTokenVO(String token, String headerName, String parameterName) {
}
