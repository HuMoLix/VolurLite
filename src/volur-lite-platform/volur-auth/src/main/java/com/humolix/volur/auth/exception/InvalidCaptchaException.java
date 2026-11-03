package com.humolix.volur.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidCaptchaException extends AuthException {

    public InvalidCaptchaException() {
        super(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
    }

    public InvalidCaptchaException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

}

