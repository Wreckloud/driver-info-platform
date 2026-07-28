package com.wreckloud.driver.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用配置校验测试。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
class AppPropertiesValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptValidProductionShapedConfiguration() {
        AppProperties properties = new AppProperties(
                new AppProperties.Admin("admin", "$2a$12$" + "a".repeat(53)),
                new AppProperties.TencentMap("map-key", "https://apis.map.qq.com/ws/geocoder/v1/"));

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectPlaceholderAdministratorPassword() {
        AppProperties properties = new AppProperties(
                new AppProperties.Admin("admin", "replace_with_bcrypt_hash"),
                new AppProperties.TencentMap("map-key", "https://apis.map.qq.com/ws/geocoder/v1/"));

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("admin.passwordBcrypt"));
    }
}
