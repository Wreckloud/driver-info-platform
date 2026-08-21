package com.wreckloud.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员修改登记参数。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "管理员修改登记参数")
public record DriverRecordUpdateRequest(
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9 ]+$", message = "项目只能包含汉字、英文字母、数字和空格")
        @Schema(description = "业务项目") String project,
        @NotBlank @Size(max = 50) String driverName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @NotBlank @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9-]{5,12}$", message = "车牌号格式不正确") String licensePlate,
        @NotBlank @Size(max = 50) String vehicleType,
        @NotBlank @Size(max = 100) @Schema(description = "数量描述，例如 20件（冻品）") String quantity,
        @NotBlank @Size(max = 200) String destination,
        @Size(max = 500) String remark) {
}
