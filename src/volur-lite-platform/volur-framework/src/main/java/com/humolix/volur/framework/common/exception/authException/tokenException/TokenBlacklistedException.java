package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class TokenBlacklistedException extends TokenAuthenticationException {

    public TokenBlacklistedException() {
        super(131305, "访问令牌已失效");
    }

}
