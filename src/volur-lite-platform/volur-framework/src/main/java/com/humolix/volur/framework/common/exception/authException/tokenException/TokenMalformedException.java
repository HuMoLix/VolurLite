package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class TokenMalformedException extends TokenAuthenticationException {

    public TokenMalformedException(Throwable cause) {
        super(131304, "访问令牌格式错误", cause);
    }

}
