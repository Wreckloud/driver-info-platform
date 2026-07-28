package com.wreckloud.driver.domain;

/**
 * 司机定位状态。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public enum LocationStatus {
    SUCCESS("定位成功"),
    DENIED("拒绝授权"),
    FAILED("定位失败"),
    TIMEOUT("定位超时"),
    NOT_REQUESTED("未尝试定位");

    private final String description;

    LocationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
