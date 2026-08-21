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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 司机提交成功照片接口。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Tag(name = "司机提交成功照片")
@Validated
@RestController
@RequestMapping("/api/driver/record-photos")
@RequiredArgsConstructor
public class DriverRecordPhotoController {
    private final DriverRecordPhotoService photoService;

    @Operation(summary = "查看本次登记照片")
    @GetMapping("/{id}")
    public ResponseEntity<?> photo(
            @Parameter(description = "照片 ID") @PathVariable @Positive Long id,
            @Parameter(description = "本次登记提交令牌")
            @RequestHeader("X-Submission-Token") UUID submissionToken) {
        DriverRecordPhotoService.PhotoContent content = photoService.loadForSubmission(
                id, submissionToken.toString());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.contentLength())
                .cacheControl(CacheControl.noStore())
                .body(content.resource());
    }
}
