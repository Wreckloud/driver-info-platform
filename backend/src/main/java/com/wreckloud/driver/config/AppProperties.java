package com.wreckloud.driver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 应用业务配置。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid Admin admin, @Valid TencentMap tencentMap) {

    public record Admin(
            @NotBlank @Size(max = 50) String username,
            @NotBlank
            @Pattern(regexp = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$",
                    message = "must be a valid BCrypt hash")
            String passwordBcrypt) {
    }

    public record TencentMap(String key, @NotBlank String endpoint) {
    }
}
