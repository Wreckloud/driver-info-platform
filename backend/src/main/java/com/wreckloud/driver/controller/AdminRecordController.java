package com.wreckloud.driver.controller;

import com.wreckloud.driver.common.ApiResult;
import com.wreckloud.driver.common.PageResult;
import com.wreckloud.driver.dto.DriverRecordQuery;
import com.wreckloud.driver.dto.DriverRecordUpdateRequest;
import com.wreckloud.driver.service.DriverRecordService;
import com.wreckloud.driver.vo.DriverRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 管理员登记记录接口。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Tag(name = "管理员登记记录")
@Validated
@RestController
@RequestMapping("/api/admin/records")
@RequiredArgsConstructor
public class AdminRecordController {
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final DriverRecordService driverRecordService;

    @Operation(summary = "分页查询登记记录")
    @GetMapping
    public ApiResult<PageResult<DriverRecordVO>> page(@Valid DriverRecordQuery query) {
        return ApiResult.success(driverRecordService.page(query));
    }

    @Operation(summary = "查看登记详情")
    @GetMapping("/{id}")
    public ApiResult<DriverRecordVO> detail(@Parameter(description = "记录 ID") @PathVariable @Positive Long id) {
        return ApiResult.success(driverRecordService.getById(id));
    }

    @Operation(summary = "修改司机填写信息")
    @PutMapping("/{id}")
    public ApiResult<DriverRecordVO> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody DriverRecordUpdateRequest request,
                                            Principal principal) {
        return ApiResult.success(driverRecordService.update(id, request, principal.getName()));
    }

    @Operation(summary = "软删除登记记录")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable @Positive Long id, Principal principal) {
        driverRecordService.delete(id, principal.getName());
        return ApiResult.success();
    }

    @Operation(summary = "导出当前筛选结果")
    @GetMapping("/export")
    public void export(@Valid DriverRecordQuery query, HttpServletResponse response) throws IOException {
        String filename = "司机出车登记_" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(FILE_TIME_FORMATTER) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build().toString());
        driverRecordService.export(query, response.getOutputStream());
    }
}
