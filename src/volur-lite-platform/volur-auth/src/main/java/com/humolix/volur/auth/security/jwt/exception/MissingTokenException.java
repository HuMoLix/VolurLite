package com.humolix.volur.auth.security.jwt.exception;

public class MissingTokenException extends RuntimeException {

    public MissingTokenException(String message) {
        super(message);
    }

}
