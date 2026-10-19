package com.humolix.volur.framework.common.api;

import com.humolix.volur.framework.common.util.MessageUtils;
import com.humolix.volur.framework.common.util.RequestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.List;

public class ApiResponseEntity<T> extends ResponseEntity<ApiResponse<T>> {

    public ApiResponseEntity(ApiResponse<T> body, HttpHeaders headers, HttpStatusCode status) {
        super(body, headers, status);
    }

    public ApiResponseEntity<T> cookie(ApiResponseCookie cookie) {
        if (cookie != null) {
            this.getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toResponseCookie().toString());
        }
        return this;
    }

    public ApiResponseEntity<T> cookies(Collection<ApiResponseCookie> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            for (ApiResponseCookie c : cookies) {
                cookie(c);
            }
        }
        return this;
    }

    public ApiResponseEntity<T> header(String headerName, String... headerValues) {
        for (String val : headerValues) {
            this.getHeaders().add(headerName, val);
        }
        return this;
    }

    public static <T> ApiResponseEntity<T> of(HttpStatusCode status, int errorCode, String messageKey, T data) {
        String translatedMessage = MessageUtils.getMessage(messageKey);
        String currentPath = RequestUtil.getOriginalRequestUri();
        ApiResponse<T> apiResponse = ApiResponse.of(
                status.value(),
                errorCode,
                status.is2xxSuccessful(),
                translatedMessage,
                data,
                currentPath
        );

        HttpHeaders headers = new HttpHeaders();
        List<ApiResponseCookie> contextCookies = CookieContextHolder.pollCookies();
        if (!contextCookies.isEmpty()) {
            for (ApiResponseCookie cookie : contextCookies) {
                headers.add(HttpHeaders.SET_COOKIE, cookie.toResponseCookie().toString());
            }
        }

        return new ApiResponseEntity<>(apiResponse, headers, status);
    }

    public static <T> ApiResponseEntity<T> of(HttpStatusCode status, String messageKey, T data) {
        return of(status, 0, messageKey, data);
    }

    public static <T> ApiResponseEntity<T> okData(T data) {
        return of(HttpStatus.OK, "response.ok", data);
    }

    public static <T> ApiResponseEntity<T> ok(String messageKey, T data) {
        return of(HttpStatus.OK, messageKey, data);
    }

    public static ApiResponseEntity<Void> success(String messageKey) {
        return of(HttpStatus.OK, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> created(T data) {
        return of(HttpStatus.CREATED, "response.created", data);
    }

    public static <T> ApiResponseEntity<T> created(String messageKey, T data) {
        return of(HttpStatus.CREATED, messageKey, data);
    }

    public static <T> ApiResponseEntity<T> badRequest(String messageKey) {
        return of(HttpStatus.BAD_REQUEST, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> badRequest(int errorCode, String messageKey) {
        return of(HttpStatus.BAD_REQUEST, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> badRequest(String messageKey, T data) {
        return of(HttpStatus.BAD_REQUEST, messageKey, data);
    }

    public static <T> ApiResponseEntity<T> badRequest(int errorCode, String messageKey, T data) {
        return of(HttpStatus.BAD_REQUEST, errorCode, messageKey, data);
    }

    public static <T> ApiResponseEntity<T> unauthorized(String messageKey) {
        return of(HttpStatus.UNAUTHORIZED, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> unauthorized(int errorCode, String messageKey) {
        return of(HttpStatus.UNAUTHORIZED, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> forbidden(String messageKey) {
        return of(HttpStatus.FORBIDDEN, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> forbidden(int errorCode, String messageKey) {
        return of(HttpStatus.FORBIDDEN, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> notFound(String messageKey) {
        return of(HttpStatus.NOT_FOUND, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> notFound(int errorCode, String messageKey) {
        return of(HttpStatus.NOT_FOUND, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> methodNotAllowed(String messageKey) {
        return of(HttpStatus.METHOD_NOT_ALLOWED, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> methodNotAllowed(int errorCode, String messageKey) {
        return of(HttpStatus.METHOD_NOT_ALLOWED, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> conflict(String messageKey) {
        return of(HttpStatus.CONFLICT, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> conflict(int errorCode, String messageKey) {
        return of(HttpStatus.CONFLICT, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> tooManyRequests(String messageKey) {
        return of(HttpStatus.TOO_MANY_REQUESTS, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> tooManyRequests(int errorCode, String messageKey) {
        return of(HttpStatus.TOO_MANY_REQUESTS, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> error(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> error(int errorCode, String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> error(HttpStatusCode status, String messageKey) {
        return of(status, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> error(HttpStatusCode status, int errorCode, String messageKey) {
        return of(status, errorCode, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> internalServerError(String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, null);
    }

    public static <T> ApiResponseEntity<T> internalServerError(int errorCode, String messageKey) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, messageKey, null);
    }
}
