package com.wreckloud.driver.service;

import com.wreckloud.driver.common.PageResult;
import com.wreckloud.driver.dto.DriverRecordCreateRequest;
import com.wreckloud.driver.dto.DriverRecordQuery;
import com.wreckloud.driver.dto.DriverRecordUpdateRequest;
import com.wreckloud.driver.vo.DriverRecordSummaryVO;
import com.wreckloud.driver.vo.DriverRecordVO;

import java.io.OutputStream;

/**
 * 司机登记业务服务。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public interface DriverRecordService {

    DriverRecordSummaryVO create(DriverRecordCreateRequest request);

    PageResult<DriverRecordVO> page(DriverRecordQuery query);

    DriverRecordVO getById(Long id);

    DriverRecordVO update(Long id, DriverRecordUpdateRequest request, String operator);

    void delete(Long id, String operator);

    void export(DriverRecordQuery query, OutputStream outputStream);
}
