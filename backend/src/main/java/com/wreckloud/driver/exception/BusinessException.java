package com.wreckloud.driver.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * @author Wreckloud
 * @since 2026-07-27
 */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
