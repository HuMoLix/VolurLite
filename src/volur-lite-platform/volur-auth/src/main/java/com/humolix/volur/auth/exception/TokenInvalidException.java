package com.humolix.volur.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidException extends AuthException {

    public TokenInvalidException() {
        super(HttpStatus.UNAUTHORIZED, "令牌错误或已过期");
    }

    public TokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

}

