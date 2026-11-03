package com.humolix.volur.framework.id.exception;

public class SnowflakeWorkerIdException extends RuntimeException {

    public SnowflakeWorkerIdException(String message) {
        super(message);
    }

    public SnowflakeWorkerIdException(String message, Throwable cause) {
        super(message, cause);
    }

}
