package com.wreckloud.driver.mapper;

import com.wreckloud.driver.domain.DriverRecord;
import com.wreckloud.driver.dto.RecordSearchCriteria;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 司机登记数据访问。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public interface DriverRecordMapper {

    int insert(DriverRecord record);

    DriverRecord findBySubmissionToken(String submissionToken);

    DriverRecord findById(Long id);

    long count(RecordSearchCriteria criteria);

    List<DriverRecord> selectPage(RecordSearchCriteria criteria);

    List<DriverRecord> selectForExport(RecordSearchCriteria criteria);

    int updateEditableFields(DriverRecord record);

    int softDelete(@Param("id") Long id,
                   @Param("operator") String operator,
                   @Param("deletedAt") LocalDateTime deletedAt);
}
