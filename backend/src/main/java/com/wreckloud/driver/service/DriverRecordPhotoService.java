package com.wreckloud.driver.service;

import com.wreckloud.driver.vo.DriverRecordPhotoVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 司机登记照片业务服务。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
public interface DriverRecordPhotoService {

    List<DriverRecordPhotoVO> save(Long recordId, List<MultipartFile> photos, LocalDateTime createdAt);

    List<DriverRecordPhotoVO> list(Long recordId);

    PhotoContent load(Long photoId);

    PhotoContent loadForSubmission(Long photoId, String submissionToken);

    record PhotoContent(Resource resource, String contentType, long contentLength) {
    }
}
