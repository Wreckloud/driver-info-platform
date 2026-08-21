package com.wreckloud.driver.dto;

import com.wreckloud.driver.domain.LocationStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 司机登记参数校验测试。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
class DriverRecordCreateRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptSuccessfulLocationWithCompleteCoordinates() {
        DriverRecordCreateRequest request = validRequest(LocationStatus.SUCCESS,
                new BigDecimal("39.9042000"), new BigDecimal("116.4074000"),
                new BigDecimal("15.50"), Instant.parse("2026-07-27T01:00:00Z"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectSuccessfulLocationWithoutCoordinates() {
        DriverRecordCreateRequest request = validRequest(LocationStatus.SUCCESS, null, null, null, null);

        assertThat(validator.validate(request)).extracting("message")
                .contains("定位状态与定位数据不一致");
    }

    @Test
    void shouldAcceptAllNonSuccessLocationStatusesWithoutCoordinates() {
        for (LocationStatus status : LocationStatus.values()) {
            if (status != LocationStatus.SUCCESS) {
                assertThat(validator.validate(validRequest(status, null, null, null, null))).isEmpty();
            }
        }
    }

    @Test
    void shouldRejectInvalidPhoneAndLicensePlate() {
        DriverRecordCreateRequest request = new DriverRecordCreateRequest(
                UUID.randomUUID(), "冷链A1", "张三", "123", "A", "货车", "20件（冻品）", "天津", null,
                LocationStatus.NOT_REQUESTED, null, null, null, null);

        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void shouldAcceptTextQuantityAndOptionalRemark() {
        DriverRecordCreateRequest request = new DriverRecordCreateRequest(
                UUID.randomUUID(), "冷链 A1", "张三", "13800138000", "京A12345", "厢式货车",
                "20件（冻品）", "天津", "需要全程冷藏", LocationStatus.NOT_REQUESTED,
                null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedProjectCharactersAndBlankQuantity() {
        DriverRecordCreateRequest request = new DriverRecordCreateRequest(
                UUID.randomUUID(), "冷链@A1", "张三", "13800138000", "京A12345", "厢式货车",
                " ", "天津", null, LocationStatus.NOT_REQUESTED, null, null, null, null);

        assertThat(validator.validate(request)).extracting("message")
                .contains("项目只能包含汉字、英文字母、数字和空格", "不能为空");
    }

    private DriverRecordCreateRequest validRequest(LocationStatus status,
                                                    BigDecimal latitude,
                                                    BigDecimal longitude,
                                                    BigDecimal accuracy,
                                                    Instant locatedAt) {
        return new DriverRecordCreateRequest(UUID.randomUUID(), "冷链A1", "张三", "13800138000", "京A12345",
                "厢式货车", "20件（冻品）", "天津", null, status, latitude, longitude, accuracy, locatedAt);
    }
}
