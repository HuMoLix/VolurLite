package com.humolix.volur.framework.common.api;

import com.humolix.volur.framework.common.util.MessageUtils;
import com.humolix.volur.framework.common.util.RequestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class ApiResponseEntity {

    private ApiResponseEntity() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static <T> ApiResponse<T> of(HttpStatusCode status, int errorCode, String messageKey, T data) {
        String translatedMessage = MessageUtils.getMessage(messageKey);
        String currentPath = RequestUtil.getOriginalRequestUri();
        return ApiResponse.of(
                status.value(),
                errorCode,
                status.is2xxSuccessful(),
                translatedMessage,
                data,
                currentPath
        );
    }

    public static <T> ApiResponse<T> of(HttpStatusCode status, String messageKey, T data) {
        return of(status, 0, messageKey, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, "response.ok", data);
    }

    public static <T> ApiResponse<T> ok(String messageKey, T data) {
        return of(HttpStatus.OK, messageKey, data);
    }

    public static ApiResponse<Void> success(String messageKey) {
        return of(HttpStatus.OK, messageKey, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return of(HttpStatus.CREATED, "response.created", data);
    }

    public static <T> ApiResponse<T> created(String messageKey, T data) {
        return of(HttpStatus.CREATED, messageKey, data);
    }

    public static <T> ApiResponse<T> badRequest(String messageKey) {
        return of(HttpStatus.BAD_REQUEST, messageKey, null);
    }

    public static <T> ApiResponse<T> badRequest(int errorCode, String messageKey) {
        return of(HttpStatus.BAD_REQUEST, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> badRequest(String messageKey, T data) {
        return of(HttpStatus.BAD_REQUEST, messageKey, data);
    }

    public static <T> ApiResponse<T> badRequest(int errorCode, String messageKey, T data) {
        return of(HttpStatus.BAD_REQUEST, errorCode, messageKey, data);
    }

    public static <T> ApiResponse<T> unauthorized(String messageKey) {
        return of(HttpStatus.UNAUTHORIZED, messageKey, null);
    }

    public static <T> ApiResponse<T> unauthorized(int errorCode, String messageKey) {
        return of(HttpStatus.UNAUTHORIZED, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> forbidden(String messageKey) {
        return of(HttpStatus.FORBIDDEN, messageKey, null);
    }

    public static <T> ApiResponse<T> forbidden(int errorCode, String messageKey) {
        return of(HttpStatus.FORBIDDEN, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> notFound(String messageKey) {
        return of(HttpStatus.NOT_FOUND, messageKey, null);
    }

    public static <T> ApiResponse<T> notFound(int errorCode, String messageKey) {
        return of(HttpStatus.NOT_FOUND, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> methodNotAllowed(String messageKey) {
        return of(HttpStatus.METHOD_NOT_ALLOWED, messageKey, null);
    }

    public static <T> ApiResponse<T> methodNotAllowed(int errorCode, String messageKey) {
        return of(HttpStatus.METHOD_NOT_ALLOWED, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> conflict(String messageKey) {
        return of(HttpStatus.CONFLICT, messageKey, null);
    }

    public static <T> ApiResponse<T> conflict(int errorCode, String messageKey) {
        return of(HttpStatus.CONFLICT, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> tooManyRequests(String messageKey) {
        return of(HttpStatus.TOO_MANY_REQUESTS, messageKey, null);
    }

    public static <T> ApiResponse<T> tooManyRequests(int errorCode, String messageKey) {
        return of(HttpStatus.TOO_MANY_REQUESTS, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> error(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    public static <T> ApiResponse<T> error(int errorCode, String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, String messageKey) {
        return of(status, messageKey, null);
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, int errorCode, String messageKey) {
        return of(status, errorCode, messageKey, null);
    }

    public static <T> ApiResponse<T> internalServerError(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    public static <T> ApiResponse<T> internalServerError(int errorCode, String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, messageKey, null);
    }

}
