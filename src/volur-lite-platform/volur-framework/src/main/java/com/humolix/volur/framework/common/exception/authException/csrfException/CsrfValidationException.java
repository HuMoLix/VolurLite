package com.humolix.volur.framework.common.exception.authException.csrfException;

import com.humolix.volur.framework.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public sealed abstract class CsrfValidationException extends BusinessException
        permits CsrfOriginDeniedException, CsrfTokenMismatchException {

    protected CsrfValidationException(int errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }

    protected CsrfValidationException(int errorCode, String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, errorCode, message, cause);
    }

}
