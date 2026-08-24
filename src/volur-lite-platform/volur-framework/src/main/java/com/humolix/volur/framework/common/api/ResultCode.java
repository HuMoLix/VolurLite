package com.humolix.volur.framework.common.api;

/**
 * Standard API Result Codes
 */
public enum ResultCode implements IErrorCode {
    SUCCESS(200, "Operation successful"),
    FAILED(500, "Operation failed"),
    VALIDATE_FAILED(400, "Parameter validation failed"),
    UNAUTHORIZED(401, "Unauthorized or token expired"),
    FORBIDDEN(403, "Access denied"),
    FLAG_INCORRECT(1001, "Flag is incorrect"),
    FLAG_ALREADY_SUBMITTED(1002, "Flag already submitted"),
    CONTAINER_START_FAILED(2001, "Failed to start dynamic container"),
    CONTAINER_NOT_FOUND(2002, "Container instance not found or expired");

    private final long code;
    private final String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public long getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
