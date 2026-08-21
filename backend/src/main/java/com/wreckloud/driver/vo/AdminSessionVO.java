package com.wreckloud.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前管理员会话信息。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Schema(description = "当前管理员会话信息")
public record AdminSessionVO(String username, boolean canManage) {
}
