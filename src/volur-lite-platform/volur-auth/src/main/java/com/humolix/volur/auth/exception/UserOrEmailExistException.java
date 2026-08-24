package com.humolix.volur.auth.exception;

public class UserOrEmailExistException extends AuthException {

    public UserOrEmailExistException() {
        super("用户名或邮箱已存在");
    }

}
