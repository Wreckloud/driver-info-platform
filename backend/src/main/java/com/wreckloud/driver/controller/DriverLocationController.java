package com.wreckloud.driver.controller;

import com.wreckloud.driver.common.ApiResult;
import com.wreckloud.driver.dto.LocationAddressRequest;
import com.wreckloud.driver.service.DriverLocationService;
import com.wreckloud.driver.vo.LocationAddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机端定位接口。
 *
 * @author Wreckloud
 * @since 2026-07-28
 */
@Tag(name = "司机定位")
@RestController
@RequestMapping("/api/driver/locations")
@RequiredArgsConstructor
public class DriverLocationController {
    private final DriverLocationService driverLocationService;

    @Operation(summary = "将经纬度解析为文字地址")
    @PostMapping("/address")
    public ApiResult<LocationAddressVO> resolveAddress(
            @Valid @RequestBody LocationAddressRequest request,
            HttpServletRequest servletRequest) {
        return ApiResult.success(driverLocationService.resolveAddress(request, servletRequest.getRemoteAddr()));
    }
}
