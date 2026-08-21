package com.wreckloud.driver.service.impl;

import com.wreckloud.driver.config.AppProperties;
import com.wreckloud.driver.domain.DriverRecordPhoto;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.mapper.DriverRecordPhotoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 司机登记照片业务测试。
 *
 * @author Wreckloud
 * @since 2026-08-21
 */
class DriverRecordPhotoServiceImplTest {
    @TempDir
    private Path temporaryDirectory;

    private DriverRecordPhotoMapper mapper;
    private DriverRecordPhotoServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(DriverRecordPhotoMapper.class);
        AppProperties properties = new AppProperties(
                new AppProperties.Admin("admin", "$2a$12$" + "a".repeat(53)),
                new AppProperties.Viewer("", ""),
                new AppProperties.Photo(temporaryDirectory.toString(), 9, 2097152, 2048, 2048),
                new AppProperties.TencentMap("", "https://apis.map.qq.com/ws/geocoder/v1/"));
        service = new DriverRecordPhotoServiceImpl(mapper, properties);
        service.initializeStorage();
    }

    @Test
    void shouldValidateAndPersistCompressedJpeg() throws Exception {
        doAnswer(invocation -> {
            invocation.<DriverRecordPhoto>getArgument(0).setId(12L);
            return 1;
        }).when(mapper).insert(any(DriverRecordPhoto.class));
        MockMultipartFile file = new MockMultipartFile(
                "photos", "trip.jpg", "image/jpeg", jpeg(800, 600));

        var result = service.save(7L, List.of(file), LocalDateTime.parse("2026-08-21T03:00:00"));

        ArgumentCaptor<DriverRecordPhoto> captor = ArgumentCaptor.forClass(DriverRecordPhoto.class);
        verify(mapper).insert(captor.capture());
        assertThat(result).singleElement().satisfies(photo -> {
            assertThat(photo.id()).isEqualTo(12L);
            assertThat(photo.width()).isEqualTo(800);
            assertThat(photo.height()).isEqualTo(600);
        });
        assertThat(temporaryDirectory.resolve("7").resolve(captor.getValue().getStorageName())).exists();
    }

    @Test
    void shouldRequireAtLeastOneAndAtMostNinePhotos() {
        assertThatThrownBy(() -> service.save(7L, List.of(), LocalDateTime.now()))
                .isInstanceOf(BusinessException.class).hasMessage("请至少上传一张照片");
        assertThatThrownBy(() -> service.save(7L,
                Collections.nCopies(10, new MockMultipartFile("photos", new byte[]{1})), LocalDateTime.now()))
                .isInstanceOf(BusinessException.class).hasMessage("最多上传 9 张照片");
    }

    @Test
    void shouldLoadPhotoForMatchingSubmissionToken() throws Exception {
        DriverRecordPhoto photo = new DriverRecordPhoto();
        photo.setId(12L);
        photo.setDriverRecordId(7L);
        photo.setStorageName("trip.jpg");
        photo.setContentType("image/jpeg");
        byte[] content = jpeg(800, 600);
        photo.setFileSize(content.length);
        Files.createDirectories(temporaryDirectory.resolve("7"));
        Files.write(temporaryDirectory.resolve("7").resolve("trip.jpg"), content);
        when(mapper.findActiveByIdAndSubmissionToken(12L, "submission-token")).thenReturn(photo);

        var result = service.loadForSubmission(12L, "submission-token");

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.contentLength()).isEqualTo(content.length);
        assertThat(result.resource().exists()).isTrue();
    }

    @Test
    void shouldRejectPhotoWhenSubmissionTokenDoesNotMatch() {
        assertThatThrownBy(() -> service.loadForSubmission(12L, "wrong-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("照片不存在或登记记录已删除");
    }

    private byte[] jpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
