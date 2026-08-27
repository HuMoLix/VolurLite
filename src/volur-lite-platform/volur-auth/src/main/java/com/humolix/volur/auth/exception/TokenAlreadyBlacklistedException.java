package com.humolix.volur.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenAlreadyBlacklistedException extends AuthException {

    public TokenAlreadyBlacklistedException() {
        super(HttpStatus.UNAUTHORIZED, "令牌已被禁用");
    }

    public TokenAlreadyBlacklistedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

}

