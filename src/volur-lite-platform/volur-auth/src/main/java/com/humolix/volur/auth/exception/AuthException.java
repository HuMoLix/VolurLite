package com.humolix.volur.auth.exception;

import com.humolix.volur.framework.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class AuthException extends BusinessException {

    public AuthException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public AuthException(HttpStatusCode status, String message) {
        super(status, message);
    }

    public AuthException(HttpStatusCode status, int errorCode, String message) {
        super(status, errorCode, message);
    }

    public AuthException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }

    public AuthException(HttpStatusCode status, String message, Throwable cause) {
        super(status, message, cause);
    }

}

