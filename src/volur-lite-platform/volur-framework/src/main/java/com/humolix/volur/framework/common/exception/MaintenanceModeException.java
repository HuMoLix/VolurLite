package com.humolix.volur.framework.common.exception;

import org.springframework.http.HttpStatus;

public class MaintenanceModeException extends BusinessException {

    public MaintenanceModeException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), message);
    }

    public MaintenanceModeException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), message, cause);
    }
}
