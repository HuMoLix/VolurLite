package com.humolix.volur.auth.exception;

public class TokenAlreadyBlacklistedException extends AuthException {

    public TokenAlreadyBlacklistedException() {
        super("令牌已被禁用");
    }

    public TokenAlreadyBlacklistedException(String message) {
        super(message);
    }

}
