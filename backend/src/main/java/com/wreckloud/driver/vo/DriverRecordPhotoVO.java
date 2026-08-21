package com.wreckloud.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理员登记照片视图。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Schema(description = "登记照片")
public record DriverRecordPhotoVO(Long id, String url, int width, int height) {
}
