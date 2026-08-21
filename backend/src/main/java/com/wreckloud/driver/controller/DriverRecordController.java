package com.wreckloud.driver.controller;

import com.wreckloud.driver.common.ApiResult;
import com.wreckloud.driver.dto.DriverRecordCreateRequest;
import com.wreckloud.driver.service.DriverRecordService;
import com.wreckloud.driver.vo.DriverRecordSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 司机登记接口。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Tag(name = "司机登记")
@RestController
@RequestMapping("/api/driver/records")
@RequiredArgsConstructor
public class DriverRecordController {
    private final DriverRecordService driverRecordService;

    @Operation(summary = "提交出车登记")
    @PostMapping(consumes = "multipart/form-data")
    public ApiResult<DriverRecordSummaryVO> create(
            @Valid @RequestPart("record") DriverRecordCreateRequest request,
            @RequestPart("photos") List<MultipartFile> photos) {
        return ApiResult.success(driverRecordService.create(request, photos));
    }
}
