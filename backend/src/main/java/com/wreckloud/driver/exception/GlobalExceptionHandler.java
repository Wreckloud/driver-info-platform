package com.wreckloud.driver.exception;

import com.wreckloud.driver.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常响应处理。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResult.error(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception exception) {
        String message;
        if (exception instanceof MethodArgumentNotValidException methodException) {
            message = methodException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else {
            message = ((BindException) exception).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(ApiResult.error(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst().map(violation -> violation.getMessage()).orElse("请求参数不正确");
        return ResponseEntity.badRequest().body(ApiResult.error(message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiResult<Void>> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResult.error("请求参数格式不正确"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResult.error("上传照片总大小超过限制"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("服务器暂时无法处理请求，请稍后重试"));
    }
}
