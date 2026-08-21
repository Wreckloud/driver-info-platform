package com.wreckloud.driver.service.impl;

import com.wreckloud.driver.config.AppProperties;
import com.wreckloud.driver.domain.DriverRecordPhoto;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.mapper.DriverRecordPhotoMapper;
import com.wreckloud.driver.service.DriverRecordPhotoService;
import com.wreckloud.driver.vo.DriverRecordPhotoVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 司机登记照片业务实现。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DriverRecordPhotoServiceImpl implements DriverRecordPhotoService {
    private static final String CONTENT_TYPE = "image/jpeg";

    private final DriverRecordPhotoMapper photoMapper;
    private final AppProperties properties;
    private Path storageRoot;

    @PostConstruct
    void initializeStorage() {
        storageRoot = Path.of(properties.photo().storagePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize photo storage", exception);
        }
    }

    @Override
    public List<DriverRecordPhotoVO> save(Long recordId, List<MultipartFile> photos, LocalDateTime createdAt) {
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请至少上传一张照片");
        }
        if (photos.size() > properties.photo().maxCount()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "最多上传 9 张照片");
        }

        List<DriverRecordPhotoVO> result = new ArrayList<>(photos.size());
        for (int index = 0; index < photos.size(); index++) {
            result.add(saveOne(recordId, photos.get(index), index, createdAt));
        }
        return result;
    }

    @Override
    public List<DriverRecordPhotoVO> list(Long recordId) {
        return photoMapper.findByRecordId(recordId).stream().map(this::toVO).toList();
    }

    @Override
    public PhotoContent load(Long photoId) {
        DriverRecordPhoto photo = photoMapper.findActiveById(photoId);
        return loadContent(photo);
    }

    @Override
    public PhotoContent loadForSubmission(Long photoId, String submissionToken) {
        DriverRecordPhoto photo = photoMapper.findActiveByIdAndSubmissionToken(photoId, submissionToken);
        return loadContent(photo);
    }

    private PhotoContent loadContent(DriverRecordPhoto photo) {
        if (photo == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "照片不存在或登记记录已删除");
        }
        Path path = resolvePhotoPath(photo.getDriverRecordId(), photo.getStorageName());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "照片文件不存在");
        }
        return new PhotoContent(resource, photo.getContentType(), photo.getFileSize());
    }

    private DriverRecordPhotoVO saveOne(Long recordId, MultipartFile file, int displayOrder,
                                        LocalDateTime createdAt) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "照片文件不能为空");
        }
        if (file.getSize() > properties.photo().maxFileSize()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "单张照片压缩后不能超过 2MB");
        }
        ImageSize imageSize = inspect(file);
        if (imageSize.width() > properties.photo().maxWidth()
                || imageSize.height() > properties.photo().maxHeight()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "照片尺寸过大，请重新选择");
        }

        String storageName = UUID.randomUUID() + ".jpg";
        Path recordDirectory = storageRoot.resolve(recordId.toString()).normalize();
        Path finalPath = resolvePhotoPath(recordId, storageName);
        Path temporaryPath = null;
        try {
            Files.createDirectories(recordDirectory);
            temporaryPath = Files.createTempFile(recordDirectory, ".upload-", ".tmp");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temporaryPath, finalPath);
            registerRollbackCleanup(finalPath);

            DriverRecordPhoto photo = new DriverRecordPhoto();
            photo.setDriverRecordId(recordId);
            photo.setStorageName(storageName);
            photo.setContentType(CONTENT_TYPE);
            photo.setFileSize(Files.size(finalPath));
            photo.setWidth(imageSize.width());
            photo.setHeight(imageSize.height());
            photo.setDisplayOrder(displayOrder);
            photo.setCreatedAt(createdAt);
            photoMapper.insert(photo);
            return toVO(photo);
        } catch (IOException exception) {
            deleteQuietly(temporaryPath);
            deleteQuietly(finalPath);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "照片保存失败，请稍后重试");
        }
    }

    private ImageSize inspect(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                if (!"JPEG".equalsIgnoreCase(reader.getFormatName())) {
                    throw invalidImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw invalidImage();
                }
                return new ImageSize(width, height);
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private BusinessException invalidImage() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "照片格式不正确，请重新选择");
    }

    private Path resolvePhotoPath(Long recordId, String storageName) {
        Path path = storageRoot.resolve(recordId.toString()).resolve(storageName).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "照片路径不正确");
        }
        return path;
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void registerRollbackCleanup(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(path);
                }
            }
        });
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Failed to remove uncommitted photo file, fileName={}", path.getFileName(), exception);
        }
    }

    private DriverRecordPhotoVO toVO(DriverRecordPhoto photo) {
        return new DriverRecordPhotoVO(photo.getId(),
                "/api/admin/record-photos/" + photo.getId(), photo.getWidth(), photo.getHeight());
    }

    private record ImageSize(int width, int height) {
    }
}
