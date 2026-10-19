package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class InvalidTokenException extends TokenAuthenticationException {

    public InvalidTokenException(String message, Throwable cause) {
        super(131302, message, cause);
    }

    public InvalidTokenException(String message) {
        super(131302, message);
    }

}
