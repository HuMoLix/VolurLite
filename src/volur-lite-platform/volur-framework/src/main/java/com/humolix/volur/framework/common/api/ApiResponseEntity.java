package com.humolix.volur.framework.common.api;

import com.humolix.volur.framework.common.util.MessageUtils;
import com.humolix.volur.framework.security.util.RequestHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class ApiResponseEntity {

    private ApiResponseEntity() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static <T> ApiResponse<T> of(HttpStatusCode status, int errorCode, String messageKey, T data) {
        String translatedMessage = MessageUtils.getMessage(messageKey);
        String currentPath = RequestHolder.getOriginalRequestUri();
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

    public static <T> ApiResponse<T> badRequest(String messageKey, T data) {
        return of(HttpStatus.BAD_REQUEST, messageKey, data);
    }

    public static <T> ApiResponse<T> unauthorized(String messageKey) {
        return of(HttpStatus.UNAUTHORIZED, messageKey, null);
    }

    public static <T> ApiResponse<T> forbidden(String messageKey) {
        return of(HttpStatus.FORBIDDEN, messageKey, null);
    }

    public static <T> ApiResponse<T> notFound(String messageKey) {
        return of(HttpStatus.NOT_FOUND, messageKey, null);
    }

    public static <T> ApiResponse<T> error(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, String messageKey) {
        return of(status, messageKey, null);
    }

    public static <T> ApiResponse<T> internalServerError(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    @Deprecated
    public static <T> ApiResponse<T> OK(T data) {
        return ok(data);
    }

    @Deprecated
    public static <T> ApiResponse<T> OK(String messageKey, T data) {
        return ok(messageKey, data);
    }

    @Deprecated
    public static ApiResponse<Void> SUCCESS(String messageKey) {
        return success(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> CREATED(T data) {
        return created(data);
    }

    @Deprecated
    public static <T> ApiResponse<T> CREATED(String messageKey, T data) {
        return created(messageKey, data);
    }

    @Deprecated
    public static <T> ApiResponse<T> BAD_REQUEST(String messageKey) {
        return badRequest(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> BAD_REQUEST(String messageKey, T data) {
        return badRequest(messageKey, data);
    }

    @Deprecated
    public static <T> ApiResponse<T> UNAUTHORIZED(String messageKey) {
        return unauthorized(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> FORBIDDEN(String messageKey) {
        return forbidden(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> NOT_FOUND(String messageKey) {
        return notFound(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> ERROR(String messageKey) {
        return error(messageKey);
    }

    @Deprecated
    public static <T> ApiResponse<T> ERROR(HttpStatusCode status, int errorCode, String messageKey) {
        return of(status, errorCode, messageKey, null);
    }

    @Deprecated
    public static <T> ApiResponse<T> INTERNAL_SERVER_ERROR(String messageKey) {
        return internalServerError(messageKey);
    }

}
