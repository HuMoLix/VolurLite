package com.humolix.volur.framework.id.exception;

public class SnowflakeClockBackwardsException extends RuntimeException {

    public SnowflakeClockBackwardsException(String message) {
        super(message);
    }

    public SnowflakeClockBackwardsException(String message, Throwable cause) {
        super(message, cause);
    }

}
