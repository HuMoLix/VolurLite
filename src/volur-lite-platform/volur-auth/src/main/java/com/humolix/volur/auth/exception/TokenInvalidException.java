package com.humolix.volur.auth.exception;

public class TokenInvalidException extends AuthException {

    public TokenInvalidException() {
        super("令牌错误或已过期");
    }

    public TokenInvalidException(String message) {
        super(message);
    }

}
