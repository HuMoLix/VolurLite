package com.humolix.volur.framework.common.exception.authException.tokenException;

import com.humolix.volur.framework.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public sealed abstract class TokenAuthenticationException extends BusinessException
    permits InvalidTokenException,
        TokenExpiredException,
        TokenMalformedException,
        TokenBlacklistedException,
        UserForceLoggedOutException,
        TokenMissingException {

    protected TokenAuthenticationException(int errorCode, String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message, cause);
    }

    protected TokenAuthenticationException(int errorCode, String message) {
        this(errorCode, message, null);
    }

}
