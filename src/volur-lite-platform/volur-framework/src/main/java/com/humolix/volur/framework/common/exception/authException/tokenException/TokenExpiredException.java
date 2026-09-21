package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class TokenExpiredException extends TokenAuthenticationException {

    public TokenExpiredException(Throwable cause) {
        super(131303, "访问令牌已过期", cause);
    }

}
