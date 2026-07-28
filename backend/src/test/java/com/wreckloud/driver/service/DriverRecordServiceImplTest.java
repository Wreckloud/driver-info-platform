package com.wreckloud.driver.service;

import com.wreckloud.driver.domain.DriverRecord;
import com.wreckloud.driver.domain.LocationStatus;
import com.wreckloud.driver.dto.DriverRecordCreateRequest;
import com.wreckloud.driver.dto.DriverRecordQuery;
import com.wreckloud.driver.mapper.DriverRecordMapper;
import com.wreckloud.driver.service.impl.DriverRecordServiceImpl;
import com.wreckloud.driver.vo.DriverRecordSummaryVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 司机登记业务测试。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@ExtendWith(MockitoExtension.class)
class DriverRecordServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-07-27T02:00:00Z");

    @Mock
    private DriverRecordMapper mapper;
    @Mock
    private ReverseGeocodingService geocodingService;

    private DriverRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DriverRecordServiceImpl(mapper, geocodingService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateRecordWithoutLocationAndUseServerTime() {
        UUID token = UUID.randomUUID();
        doAnswer(invocation -> {
            invocation.<DriverRecord>getArgument(0).setId(10L);
            return 1;
        }).when(mapper).insert(any(DriverRecord.class));

        DriverRecordSummaryVO result = service.create(new DriverRecordCreateRequest(
                token, " 张三 ", "13800138000", "京a12345", " 厢式货车 ", " 天津 ",
                LocationStatus.NOT_REQUESTED, null, null, null, null));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.licensePlate()).isEqualTo("京A12345");
        assertThat(result.createdAt()).isEqualTo(NOW);
        verify(geocodingService, never()).resolveAddress(any(), any());
    }

    @Test
    void shouldReturnExistingRecordForRepeatedSubmissionToken() {
        DriverRecord existing = sampleRecord();
        when(mapper.findBySubmissionToken(existing.getSubmissionToken())).thenReturn(existing);

        DriverRecordSummaryVO result = service.create(new DriverRecordCreateRequest(
                UUID.fromString(existing.getSubmissionToken()), "李四", "13900139000", "沪B88888", "货车", "苏州",
                LocationStatus.NOT_REQUESTED, null, null, null, null));

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.locationAddress()).isEqualTo("北京市东城区");
        assertThat(result.latitude()).isEqualByComparingTo("39.9042000");
        assertThat(result.longitude()).isEqualByComparingTo("116.4074000");
        assertThat(result.locationAccuracy()).isEqualByComparingTo("15.50");
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldReturnServerTimeWithRecordPage() {
        when(mapper.count(any())).thenReturn(0L);

        var result = service.page(new DriverRecordQuery());

        assertThat(result.serverTime()).isEqualTo(NOW);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void shouldExportChineseHeadersAndRecord() throws Exception {
        when(mapper.selectForExport(any())).thenReturn(List.of(sampleRecord()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.export(new com.wreckloud.driver.dto.DriverRecordQuery(), output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("司机姓名");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("张三");
        }
    }

    private DriverRecord sampleRecord() {
        DriverRecord record = new DriverRecord();
        record.setId(8L);
        record.setSubmissionToken("ec88439a-32f0-4750-bae8-60160f4bf174");
        record.setDriverName("张三");
        record.setPhone("13800138000");
        record.setLicensePlate("京A12345");
        record.setVehicleType("厢式货车");
        record.setDestination("天津");
        record.setLocationStatus(LocationStatus.SUCCESS);
        record.setLatitude(new BigDecimal("39.9042000"));
        record.setLongitude(new BigDecimal("116.4074000"));
        record.setLocationAccuracy(new BigDecimal("15.50"));
        record.setLocationAddress("北京市东城区");
        record.setLocatedAt(java.time.LocalDateTime.ofInstant(NOW.minusSeconds(30), ZoneOffset.UTC));
        record.setCreatedAt(java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        record.setUpdatedAt(java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        return record;
    }
}
