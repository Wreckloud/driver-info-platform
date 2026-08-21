package com.wreckloud.driver.service.impl;

import com.wreckloud.driver.common.PageResult;
import com.wreckloud.driver.domain.DriverRecord;
import com.wreckloud.driver.domain.LocationStatus;
import com.wreckloud.driver.dto.DriverRecordCreateRequest;
import com.wreckloud.driver.dto.DriverRecordQuery;
import com.wreckloud.driver.dto.DriverRecordUpdateRequest;
import com.wreckloud.driver.dto.RecordSearchCriteria;
import com.wreckloud.driver.exception.BusinessException;
import com.wreckloud.driver.mapper.DriverRecordMapper;
import com.wreckloud.driver.service.DriverRecordPhotoService;
import com.wreckloud.driver.service.DriverRecordService;
import com.wreckloud.driver.service.ReverseGeocodingService;
import com.wreckloud.driver.vo.DriverRecordPhotoSummaryVO;
import com.wreckloud.driver.vo.DriverRecordPhotoVO;
import com.wreckloud.driver.vo.DriverRecordSummaryVO;
import com.wreckloud.driver.vo.DriverRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 司机登记业务实现。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverRecordServiceImpl implements DriverRecordService {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EXCEL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] EXCEL_HEADERS = {
            "记录ID", "项目", "司机姓名", "手机号", "车牌号", "车型", "数量", "目的地", "备注", "照片数量", "定位状态",
            "起始位置", "纬度", "经度", "定位精度（米）", "定位获取时间", "发车时间", "最后修改时间"
    };

    private final DriverRecordMapper driverRecordMapper;
    private final ReverseGeocodingService reverseGeocodingService;
    private final DriverRecordPhotoService photoService;
    private final Clock clock;

    @Override
    @Transactional
    public DriverRecordSummaryVO create(DriverRecordCreateRequest request, List<MultipartFile> photos) {
        String submissionToken = request.submissionToken().toString();
        DriverRecord existing = driverRecordMapper.findBySubmissionToken(submissionToken);
        if (existing != null) {
            return toSummary(existing, photoService.list(existing.getId()));
        }

        DriverRecord record = new DriverRecord();
        record.setSubmissionToken(submissionToken);
        record.setProject(trim(request.project()));
        record.setDriverName(trim(request.driverName()));
        record.setPhone(trim(request.phone()));
        record.setLicensePlate(trim(request.licensePlate()).toUpperCase());
        record.setVehicleType(trim(request.vehicleType()));
        record.setQuantity(trim(request.quantity()));
        record.setDestination(trim(request.destination()));
        record.setRemark(trimToNull(request.remark()));
        record.setLocationStatus(request.locationStatus());
        if (request.locationStatus() == LocationStatus.SUCCESS) {
            record.setLatitude(request.latitude());
            record.setLongitude(request.longitude());
            record.setLocationAccuracy(request.locationAccuracy());
            record.setLocatedAt(LocalDateTime.ofInstant(request.locatedAt(), ZoneOffset.UTC));
            record.setLocationAddress(reverseGeocodingService.resolveAddress(
                    request.latitude(), request.longitude()));
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            driverRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            DriverRecord duplicated = driverRecordMapper.findBySubmissionToken(submissionToken);
            if (duplicated != null) {
                return toSummary(duplicated, photoService.list(duplicated.getId()));
            }
            throw exception;
        }
        List<DriverRecordPhotoVO> savedPhotos = photoService.save(record.getId(), photos, now);
        record.setPhotoCount(photos.size());
        log.info("Driver record created, recordId={}, locationStatus={}", record.getId(), record.getLocationStatus());
        return toSummary(record, savedPhotos);
    }

    @Override
    public PageResult<DriverRecordVO> page(DriverRecordQuery query) {
        validateDateRange(query);
        RecordSearchCriteria criteria = toCriteria(query, true);
        long total = driverRecordMapper.count(criteria);
        List<DriverRecordVO> items = total == 0
                ? List.of()
                : driverRecordMapper.selectPage(criteria).stream().map(record -> toVO(record, false)).toList();
        return new PageResult<>(total, query.getPage(), query.getPageSize(), items, clock.instant());
    }

    @Override
    public DriverRecordVO getById(Long id) {
        return toVO(requireActiveRecord(id), true);
    }

    @Override
    public DriverRecordVO update(Long id, DriverRecordUpdateRequest request, String operator) {
        requireActiveRecord(id);
        DriverRecord record = new DriverRecord();
        record.setId(id);
        record.setProject(trim(request.project()));
        record.setDriverName(trim(request.driverName()));
        record.setPhone(trim(request.phone()));
        record.setLicensePlate(trim(request.licensePlate()).toUpperCase());
        record.setVehicleType(trim(request.vehicleType()));
        record.setQuantity(trim(request.quantity()));
        record.setDestination(trim(request.destination()));
        record.setRemark(trimToNull(request.remark()));
        record.setUpdatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        record.setUpdatedBy(operator);
        if (driverRecordMapper.updateEditableFields(record) == 0) {
            throw notFound();
        }
        log.info("Driver record updated, recordId={}, operator={}", id, operator);
        return toVO(requireActiveRecord(id), true);
    }

    @Override
    public void delete(Long id, String operator) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (driverRecordMapper.softDelete(id, operator, now) == 0) {
            throw notFound();
        }
        log.info("Driver record soft deleted, recordId={}, operator={}", id, operator);
    }

    @Override
    public void export(DriverRecordQuery query, OutputStream outputStream) {
        validateDateRange(query);
        List<DriverRecord> records = driverRecordMapper.selectForExport(toCriteria(query, false));
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("出车登记");
            writeHeader(workbook, sheet);
            for (int index = 0; index < records.size(); index++) {
                writeRecord(sheet.createRow(index + 1), records.get(index));
            }
            int[] widths = {12, 18, 14, 16, 16, 14, 18, 24, 30, 12, 14, 40, 16, 16, 18, 22, 22, 22};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            workbook.write(outputStream);
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出 Excel 失败");
        }
    }

    private void validateDateRange(DriverRecordQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    private RecordSearchCriteria toCriteria(DriverRecordQuery query, boolean paged) {
        LocalDateTime startTime = query.getStartDate() == null ? null
                : LocalDateTime.ofInstant(query.getStartDate().atStartOfDay(DISPLAY_ZONE).toInstant(), ZoneOffset.UTC);
        LocalDateTime endTime = query.getEndDate() == null ? null
                : LocalDateTime.ofInstant(query.getEndDate().plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant(), ZoneOffset.UTC);
        String keyword = StringUtils.hasText(query.getKeyword()) ? escapeLike(query.getKeyword().trim()) : null;
        int offset = paged ? (query.getPage() - 1) * query.getPageSize() : 0;
        int limit = paged ? query.getPageSize() : Integer.MAX_VALUE;
        return new RecordSearchCriteria(startTime, endTime, keyword, offset, limit);
    }

    private DriverRecord requireActiveRecord(Long id) {
        DriverRecord record = driverRecordMapper.findById(id);
        if (record == null) {
            throw notFound();
        }
        return record;
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "登记记录不存在或已删除");
    }

    private DriverRecordSummaryVO toSummary(DriverRecord record, List<DriverRecordPhotoVO> photos) {
        return new DriverRecordSummaryVO(record.getId(), record.getProject(), record.getDriverName(),
                record.getPhone(), record.getLicensePlate(), record.getVehicleType(), record.getQuantity(),
                record.getDestination(), record.getRemark(), record.getLocationStatus(), record.getLatitude(),
                record.getLongitude(), record.getLocationAddress(), record.getLocationAccuracy(),
                record.getPhotoCount(), photos.stream()
                        .map(photo -> new DriverRecordPhotoSummaryVO(photo.id(), photo.width(), photo.height()))
                        .toList(), toInstant(record.getCreatedAt()));
    }

    private DriverRecordVO toVO(DriverRecord record, boolean includePhotos) {
        return new DriverRecordVO(record.getId(), record.getProject(), record.getDriverName(), record.getPhone(),
                record.getLicensePlate(), record.getVehicleType(), record.getQuantity(), record.getDestination(),
                record.getRemark(), record.getLatitude(), record.getLongitude(), record.getLocationAddress(),
                record.getLocationAccuracy(), record.getLocationStatus(), toInstant(record.getLocatedAt()),
                toInstant(record.getCreatedAt()), toInstant(record.getUpdatedAt()), record.getUpdatedBy(),
                record.getPhotoCount(), includePhotos ? photoService.list(record.getId()) : List.of());
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void writeHeader(SXSSFWorkbook workbook, Sheet sheet) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Row row = sheet.createRow(0);
        for (int index = 0; index < EXCEL_HEADERS.length; index++) {
            row.createCell(index).setCellValue(EXCEL_HEADERS[index]);
            row.getCell(index).setCellStyle(style);
        }
    }

    private void writeRecord(Row row, DriverRecord record) {
        String[] values = {
                String.valueOf(record.getId()), record.getProject(), record.getDriverName(), record.getPhone(),
                record.getLicensePlate(), record.getVehicleType(), record.getQuantity(), record.getDestination(),
                nullToEmpty(record.getRemark()), String.valueOf(record.getPhotoCount()),
                record.getLocationStatus().getDescription(), nullToEmpty(record.getLocationAddress()),
                valueOf(record.getLatitude()), valueOf(record.getLongitude()), valueOf(record.getLocationAccuracy()),
                formatTime(record.getLocatedAt()), formatTime(record.getCreatedAt()), formatTime(record.getUpdatedAt())
        };
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(values[index]);
        }
    }

    private String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(DISPLAY_ZONE).format(EXCEL_TIME_FORMATTER);
    }
}
