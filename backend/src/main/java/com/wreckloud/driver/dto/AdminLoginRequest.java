package com.wreckloud.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员登录参数。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "管理员登录参数")
public record AdminLoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String password) {
}
