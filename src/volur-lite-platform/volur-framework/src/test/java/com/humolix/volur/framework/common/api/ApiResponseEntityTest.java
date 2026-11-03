package com.humolix.volur.framework.common.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseEntityTest {

    @AfterEach
    void tearDown() {
        CookieContextHolder.clear();
    }

    @Test
    @DisplayName("Test ok and success response helpers")
    void testOkAndSuccess() {
        ApiResponseEntity<String> okWithData = ApiResponseEntity.okData("hello");
        assertEquals(HttpStatus.OK, okWithData.getStatusCode());
        ApiResponse<String> body1 = okWithData.getBody();
        assertNotNull(body1);
        assertEquals(200, body1.status());
        assertEquals(0, body1.errorCode());
        assertTrue(body1.success());
        assertEquals("hello", body1.data());

        ApiResponseEntity<String> okWithMsg = ApiResponseEntity.ok("custom.key", "world");
        assertEquals(HttpStatus.OK, okWithMsg.getStatusCode());
        ApiResponse<String> body2 = okWithMsg.getBody();
        assertNotNull(body2);
        assertEquals(200, body2.status());
        assertEquals(0, body2.errorCode());
        assertTrue(body2.success());
        assertEquals("world", body2.data());

        ApiResponseEntity<Void> success = ApiResponseEntity.success("op.done");
        assertEquals(HttpStatus.OK, success.getStatusCode());
        ApiResponse<Void> body3 = success.getBody();
        assertNotNull(body3);
        assertEquals(200, body3.status());
        assertEquals(0, body3.errorCode());
        assertTrue(body3.success());
        assertNull(body3.data());
    }

    @Test
    @DisplayName("Test created response helpers")
    void testCreated() {
        ApiResponseEntity<Long> created1 = ApiResponseEntity.created(1001L);
        assertEquals(HttpStatus.CREATED, created1.getStatusCode());
        ApiResponse<Long> body1 = created1.getBody();
        assertNotNull(body1);
        assertEquals(201, body1.status());
        assertEquals(0, body1.errorCode());
        assertTrue(body1.success());
        assertEquals(1001L, body1.data());

        ApiResponseEntity<Long> created2 = ApiResponseEntity.created("resource.created", 1002L);
        assertEquals(HttpStatus.CREATED, created2.getStatusCode());
        ApiResponse<Long> body2 = created2.getBody();
        assertNotNull(body2);
        assertEquals(201, body2.status());
        assertEquals(0, body2.errorCode());
        assertTrue(body2.success());
        assertEquals(1002L, body2.data());
    }

    @Test
    @DisplayName("Test badRequest with and without errorCode")
    void testBadRequest() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.badRequest("param.invalid");
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(400, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.badRequest(231301, "param.invalid");
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(400, body2.status());
        assertEquals(231301, body2.errorCode());
        assertFalse(body2.success());

        ApiResponseEntity<String> res3 = ApiResponseEntity.badRequest("param.invalid", "extraInfo");
        assertEquals(HttpStatus.BAD_REQUEST, res3.getStatusCode());
        ApiResponse<String> body3 = res3.getBody();
        assertNotNull(body3);
        assertEquals(400, body3.status());
        assertEquals(0, body3.errorCode());
        assertEquals("extraInfo", body3.data());

        ApiResponseEntity<String> res4 = ApiResponseEntity.badRequest(231302, "param.invalid", "extraInfo");
        assertEquals(HttpStatus.BAD_REQUEST, res4.getStatusCode());
        ApiResponse<String> body4 = res4.getBody();
        assertNotNull(body4);
        assertEquals(400, body4.status());
        assertEquals(231302, body4.errorCode());
        assertEquals("extraInfo", body4.data());
    }

    @Test
    @DisplayName("Test unauthorized with and without errorCode")
    void testUnauthorized() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.unauthorized("auth.failed");
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(401, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.unauthorized(131301, "token.missing");
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(401, body2.status());
        assertEquals(131301, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test forbidden with and without errorCode")
    void testForbidden() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.forbidden("access.denied");
        assertEquals(HttpStatus.FORBIDDEN, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(403, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.forbidden(141201, "access.denied");
        assertEquals(HttpStatus.FORBIDDEN, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(403, body2.status());
        assertEquals(141201, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test notFound with and without errorCode")
    void testNotFound() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.notFound("user.not_found");
        assertEquals(HttpStatus.NOT_FOUND, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(404, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.notFound(252301, "user.not_found");
        assertEquals(HttpStatus.NOT_FOUND, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(404, body2.status());
        assertEquals(252301, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test methodNotAllowed with and without errorCode")
    void testMethodNotAllowed() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.methodNotAllowed("method.not_allowed");
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(405, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.methodNotAllowed(210301, "method.not_allowed");
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(405, body2.status());
        assertEquals(210301, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test conflict with and without errorCode")
    void testConflict() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.conflict("resource.conflict");
        assertEquals(HttpStatus.CONFLICT, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(409, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.conflict(252302, "username.conflict");
        assertEquals(HttpStatus.CONFLICT, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(409, body2.status());
        assertEquals(252302, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test tooManyRequests with and without errorCode")
    void testTooManyRequests() {
        ApiResponseEntity<Void> res1 = ApiResponseEntity.tooManyRequests("rate.limit");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, res1.getStatusCode());
        ApiResponse<Void> body1 = res1.getBody();
        assertNotNull(body1);
        assertEquals(429, body1.status());
        assertEquals(0, body1.errorCode());
        assertFalse(body1.success());

        ApiResponseEntity<Void> res2 = ApiResponseEntity.tooManyRequests(120301, "rate.limit");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, res2.getStatusCode());
        ApiResponse<Void> body2 = res2.getBody();
        assertNotNull(body2);
        assertEquals(429, body2.status());
        assertEquals(120301, body2.errorCode());
        assertFalse(body2.success());
    }

    @Test
    @DisplayName("Test internalServerError and error overloads")
    void testErrorAndInternalServerError() {
        ApiResponseEntity<Void> err1 = ApiResponseEntity.error("server.error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, err1.getStatusCode());
        ApiResponse<Void> body1 = err1.getBody();
        assertNotNull(body1);
        assertEquals(500, body1.status());
        assertEquals(0, body1.errorCode());

        ApiResponseEntity<Void> err2 = ApiResponseEntity.error(110101, "server.panic");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, err2.getStatusCode());
        ApiResponse<Void> body2 = err2.getBody();
        assertNotNull(body2);
        assertEquals(500, body2.status());
        assertEquals(110101, body2.errorCode());

        ApiResponseEntity<Void> err3 = ApiResponseEntity.error(HttpStatus.BAD_GATEWAY, "gateway.error");
        assertEquals(HttpStatus.BAD_GATEWAY, err3.getStatusCode());
        ApiResponse<Void> body3 = err3.getBody();
        assertNotNull(body3);
        assertEquals(502, body3.status());
        assertEquals(0, body3.errorCode());

        ApiResponseEntity<Void> err4 = ApiResponseEntity.error(HttpStatus.SERVICE_UNAVAILABLE, 170101, "service.unavailable");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, err4.getStatusCode());
        ApiResponse<Void> body4 = err4.getBody();
        assertNotNull(body4);
        assertEquals(503, body4.status());
        assertEquals(170101, body4.errorCode());

        ApiResponseEntity<Void> is1 = ApiResponseEntity.internalServerError("db.error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, is1.getStatusCode());
        ApiResponse<Void> body5 = is1.getBody();
        assertNotNull(body5);
        assertEquals(500, body5.status());
        assertEquals(0, body5.errorCode());

        ApiResponseEntity<Void> is2 = ApiResponseEntity.internalServerError(110102, "db.deadlock");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, is2.getStatusCode());
        ApiResponse<Void> body6 = is2.getBody();
        assertNotNull(body6);
        assertEquals(500, body6.status());
        assertEquals(110102, body6.errorCode());
    }

    @Test
    @DisplayName("Test cookie support and CookieContextHolder integration")
    void testCookieSupport() {
        ApiResponseCookie cookie = ApiResponseCookie.ofHttpOnly("test_token", "jwt123", Duration.ofHours(2), true);
        ApiResponseEntity<String> response = ApiResponseEntity.ok("login.success", "user1")
                .cookie(cookie)
                .header("X-Custom-Trace", "trace-999");

        assertNotNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("test_token=jwt123"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertEquals("trace-999", response.getHeaders().getFirst("X-Custom-Trace"));

        CookieContextHolder.setCookie(ApiResponseCookie.of("context_cookie", "val_abc", Duration.ofMinutes(10)));
        ApiResponseEntity<Void> contextResponse = ApiResponseEntity.success("op.ok");
        assertNotNull(contextResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        String contextSetCookie = contextResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(contextSetCookie);
        assertTrue(contextSetCookie.contains("context_cookie=val_abc"));
    }

}
