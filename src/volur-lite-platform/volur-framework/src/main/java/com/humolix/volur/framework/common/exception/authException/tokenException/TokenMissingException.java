package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class TokenMissingException extends TokenAuthenticationException {

    public TokenMissingException() {
        super(131301, "请求中缺少访问令牌");
    }

}
