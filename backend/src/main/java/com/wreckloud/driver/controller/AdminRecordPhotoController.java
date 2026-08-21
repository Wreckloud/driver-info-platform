package com.wreckloud.driver.controller;

import com.wreckloud.driver.service.DriverRecordPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员登记照片接口。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Tag(name = "管理员登记照片")
@Validated
@RestController
@RequestMapping("/api/admin/record-photos")
@RequiredArgsConstructor
public class AdminRecordPhotoController {
    private final DriverRecordPhotoService photoService;

    @Operation(summary = "查看登记照片")
    @GetMapping("/{id}")
    public ResponseEntity<?> photo(@Parameter(description = "照片 ID") @PathVariable @Positive Long id) {
        DriverRecordPhotoService.PhotoContent content = photoService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.contentLength())
                .cacheControl(CacheControl.noStore())
                .body(content.resource());
    }
}
