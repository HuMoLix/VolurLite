package com.humolix.volur.framework.common.api;

public record ApiResponse<T>(
        int status,
        int errorCode,
        boolean success,
        String message,
        T data,
        long timestamp,
        String path) {

    public ApiResponse {
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
    }

    public static <T> ApiResponse<T> of(int status, int errorCode, boolean success, String message, T data, String path) {
        return new ApiResponse<>(status, errorCode, success, message, data, 0, path);
    }
}
