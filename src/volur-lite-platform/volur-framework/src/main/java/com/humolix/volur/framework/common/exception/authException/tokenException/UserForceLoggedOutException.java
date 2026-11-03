package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class UserForceLoggedOutException extends TokenAuthenticationException {

    public UserForceLoggedOutException() {
        super(131306, "账号已在其他设备登录");
    }

}
