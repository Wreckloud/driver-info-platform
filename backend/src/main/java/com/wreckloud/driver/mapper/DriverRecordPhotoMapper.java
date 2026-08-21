package com.wreckloud.driver.mapper;

import com.wreckloud.driver.domain.DriverRecordPhoto;

import java.util.List;

/**
 * 司机登记照片数据访问。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
public interface DriverRecordPhotoMapper {

    int insert(DriverRecordPhoto photo);

    List<DriverRecordPhoto> findByRecordId(Long recordId);

    DriverRecordPhoto findActiveById(Long id);
}
