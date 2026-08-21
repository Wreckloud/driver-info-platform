package com.wreckloud.driver.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 司机出车登记记录。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Data
public class DriverRecord {
    private Long id;
    private String submissionToken;
    private String project;
    private String driverName;
    private String phone;
    private String licensePlate;
    private String vehicleType;
    private String quantity;
    private String destination;
    private String remark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationAddress;
    private BigDecimal locationAccuracy;
    private LocationStatus locationStatus;
    private LocalDateTime locatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private int photoCount;
}
