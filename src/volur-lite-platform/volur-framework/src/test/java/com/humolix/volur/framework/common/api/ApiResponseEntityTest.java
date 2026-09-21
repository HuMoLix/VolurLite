package com.humolix.volur.framework.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseEntityTest {

    @Test
    @DisplayName("Test ok and success response helpers")
    void testOkAndSuccess() {
        ApiResponse<String> okWithData = ApiResponseEntity.ok("hello");
        assertEquals(200, okWithData.status());
        assertEquals(0, okWithData.errorCode());
        assertTrue(okWithData.success());
        assertEquals("hello", okWithData.data());

        ApiResponse<String> okWithMsg = ApiResponseEntity.ok("custom.key", "world");
        assertEquals(200, okWithMsg.status());
        assertEquals(0, okWithMsg.errorCode());
        assertTrue(okWithMsg.success());
        assertEquals("world", okWithMsg.data());

        ApiResponse<Void> success = ApiResponseEntity.success("op.done");
        assertEquals(200, success.status());
        assertEquals(0, success.errorCode());
        assertTrue(success.success());
        assertNull(success.data());
    }

    @Test
    @DisplayName("Test created response helpers")
    void testCreated() {
        ApiResponse<Long> created1 = ApiResponseEntity.created(1001L);
        assertEquals(201, created1.status());
        assertEquals(0, created1.errorCode());
        assertTrue(created1.success());
        assertEquals(1001L, created1.data());

        ApiResponse<Long> created2 = ApiResponseEntity.created("resource.created", 1002L);
        assertEquals(201, created2.status());
        assertEquals(0, created2.errorCode());
        assertTrue(created2.success());
        assertEquals(1002L, created2.data());
    }

    @Test
    @DisplayName("Test badRequest with and without errorCode")
    void testBadRequest() {
        ApiResponse<Void> res1 = ApiResponseEntity.badRequest("param.invalid");
        assertEquals(400, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.badRequest(231301, "param.invalid");
        assertEquals(400, res2.status());
        assertEquals(231301, res2.errorCode());
        assertFalse(res2.success());

        ApiResponse<String> res3 = ApiResponseEntity.badRequest("param.invalid", "extraInfo");
        assertEquals(400, res3.status());
        assertEquals(0, res3.errorCode());
        assertEquals("extraInfo", res3.data());

        ApiResponse<String> res4 = ApiResponseEntity.badRequest(231302, "param.invalid", "extraInfo");
        assertEquals(400, res4.status());
        assertEquals(231302, res4.errorCode());
        assertEquals("extraInfo", res4.data());
    }

    @Test
    @DisplayName("Test unauthorized with and without errorCode")
    void testUnauthorized() {
        ApiResponse<Void> res1 = ApiResponseEntity.unauthorized("auth.failed");
        assertEquals(401, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.unauthorized(131301, "token.missing");
        assertEquals(401, res2.status());
        assertEquals(131301, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test forbidden with and without errorCode")
    void testForbidden() {
        ApiResponse<Void> res1 = ApiResponseEntity.forbidden("access.denied");
        assertEquals(403, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.forbidden(141201, "access.denied");
        assertEquals(403, res2.status());
        assertEquals(141201, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test notFound with and without errorCode")
    void testNotFound() {
        ApiResponse<Void> res1 = ApiResponseEntity.notFound("user.not_found");
        assertEquals(404, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.notFound(252301, "user.not_found");
        assertEquals(404, res2.status());
        assertEquals(252301, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test methodNotAllowed with and without errorCode")
    void testMethodNotAllowed() {
        ApiResponse<Void> res1 = ApiResponseEntity.methodNotAllowed("method.not_allowed");
        assertEquals(405, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.methodNotAllowed(210301, "method.not_allowed");
        assertEquals(405, res2.status());
        assertEquals(210301, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test conflict with and without errorCode")
    void testConflict() {
        ApiResponse<Void> res1 = ApiResponseEntity.conflict("resource.conflict");
        assertEquals(409, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.conflict(252302, "username.conflict");
        assertEquals(409, res2.status());
        assertEquals(252302, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test tooManyRequests with and without errorCode")
    void testTooManyRequests() {
        ApiResponse<Void> res1 = ApiResponseEntity.tooManyRequests("rate.limit");
        assertEquals(429, res1.status());
        assertEquals(0, res1.errorCode());
        assertFalse(res1.success());

        ApiResponse<Void> res2 = ApiResponseEntity.tooManyRequests(120301, "rate.limit");
        assertEquals(429, res2.status());
        assertEquals(120301, res2.errorCode());
        assertFalse(res2.success());
    }

    @Test
    @DisplayName("Test internalServerError and error overloads")
    void testErrorAndInternalServerError() {
        ApiResponse<Void> err1 = ApiResponseEntity.error("server.error");
        assertEquals(500, err1.status());
        assertEquals(0, err1.errorCode());

        ApiResponse<Void> err2 = ApiResponseEntity.error(110101, "server.panic");
        assertEquals(500, err2.status());
        assertEquals(110101, err2.errorCode());

        ApiResponse<Void> err3 = ApiResponseEntity.error(HttpStatus.BAD_GATEWAY, "gateway.error");
        assertEquals(502, err3.status());
        assertEquals(0, err3.errorCode());

        ApiResponse<Void> err4 = ApiResponseEntity.error(HttpStatus.SERVICE_UNAVAILABLE, 170101, "service.unavailable");
        assertEquals(503, err4.status());
        assertEquals(170101, err4.errorCode());

        ApiResponse<Void> is1 = ApiResponseEntity.internalServerError("db.error");
        assertEquals(500, is1.status());
        assertEquals(0, is1.errorCode());

        ApiResponse<Void> is2 = ApiResponseEntity.internalServerError(110102, "db.deadlock");
        assertEquals(500, is2.status());
        assertEquals(110102, is2.errorCode());
    }

}
