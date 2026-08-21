package com.wreckloud.driver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public record AppProperties(@Valid Admin admin,
                            @Valid Viewer viewer,
                            @Valid Photo photo,
                            @Valid TencentMap tencentMap) {

    public record Admin(
            @NotBlank @Size(max = 50) String username,
            @NotBlank
            @Pattern(regexp = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$",
                    message = "must be a valid BCrypt hash")
            String passwordBcrypt) {
    }

    public record Viewer(@Size(max = 50) String username, String passwordBcrypt) {

        @AssertTrue(message = "viewer credentials must both be empty or contain a username and valid BCrypt hash")
        public boolean isConfigurationValid() {
            boolean hasUsername = username != null && !username.isBlank();
            boolean hasPassword = passwordBcrypt != null && !passwordBcrypt.isBlank();
            if (!hasUsername && !hasPassword) {
                return true;
            }
            return hasUsername && hasPassword
                    && passwordBcrypt.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
        }

        public boolean enabled() {
            return username != null && !username.isBlank();
        }
    }

    public record Photo(
            @NotBlank String storagePath,
            @Min(1) @Max(9) int maxCount,
            @Min(1024) long maxFileSize,
            @Min(1) int maxWidth,
            @Min(1) int maxHeight) {
    }

    public record TencentMap(String key, @NotBlank String endpoint) {
    }
}
