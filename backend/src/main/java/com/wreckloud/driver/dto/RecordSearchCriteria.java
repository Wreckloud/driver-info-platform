package com.wreckloud.driver.dto;

import java.time.LocalDateTime;

/**
 * Mapper 使用的登记查询条件。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public record RecordSearchCriteria(
        LocalDateTime startTime,
        LocalDateTime endTimeExclusive,
        String keyword,
        int offset,
        int limit) {
}
