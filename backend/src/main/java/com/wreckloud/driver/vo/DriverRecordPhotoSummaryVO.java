package com.wreckloud.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 司机提交成功照片摘要。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Schema(description = "司机提交成功照片摘要")
public record DriverRecordPhotoSummaryVO(Long id, int width, int height) {
}
