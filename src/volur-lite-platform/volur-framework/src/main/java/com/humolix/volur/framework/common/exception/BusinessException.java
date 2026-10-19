package com.humolix.volur.framework.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatusCode status;
    private final int errorCode;

    public BusinessException(String message) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public BusinessException(int errorCode, String message) {
        this(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public BusinessException(HttpStatusCode status, String message) {
        this(status, status.value(), message);
    }

    public BusinessException(HttpStatusCode status, int errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusinessException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = status.value();
    }

    public BusinessException(HttpStatusCode status, int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

}
