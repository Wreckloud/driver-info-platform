package com.wreckloud.driver.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 司机登记照片元数据。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Data
public class DriverRecordPhoto {
    private Long id;
    private Long driverRecordId;
    private String storageName;
    private String contentType;
    private long fileSize;
    private int width;
    private int height;
    private int displayOrder;
    private LocalDateTime createdAt;
}
