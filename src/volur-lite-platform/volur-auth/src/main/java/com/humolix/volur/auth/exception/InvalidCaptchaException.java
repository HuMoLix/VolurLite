package com.humolix.volur.auth.exception;

import com.humolix.volur.framework.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCaptchaException extends BusinessException {

    public InvalidCaptchaException() {
        super(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
    }

    public InvalidCaptchaException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

}
