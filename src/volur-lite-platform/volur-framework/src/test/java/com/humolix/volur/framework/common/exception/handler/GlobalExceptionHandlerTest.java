package com.humolix.volur.framework.common.exception.handler;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Test BusinessException handling")
    void testBusinessException() {
        BusinessException ex = new BusinessException(HttpStatus.NOT_FOUND, 40401, "题目不存在");
        ApiResponseEntity<Void> entity = handler.handleBusinessException(ex);
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(404, response.status());
        assertEquals(40401, response.errorCode());
        assertFalse(response.success());
        assertEquals("题目不存在", response.message());
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test BadCredentialsException handling")
    void testBadCredentialsException() {
        ApiResponseEntity<Void> entity = handler.handleBadCredentialsException();
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(401, response.status());
        assertFalse(response.success());
        assertEquals("用户名或密码错误", response.message());
        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test AccessDeniedException handling")
    void testAccessDeniedException() {
        ApiResponseEntity<Void> entity = handler.handleAccessDeniedException();
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(403, response.status());
        assertFalse(response.success());
        assertEquals("权限不足，拒绝访问", response.message());
        assertEquals(HttpStatus.FORBIDDEN, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test NoResourceFoundException handling")
    void testNoResourceFoundException() {
        ApiResponseEntity<Void> entity = handler.handleNoResourceFoundException();
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(404, response.status());
        assertFalse(response.success());
        assertEquals("访问的资源不存在", response.message());
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test HttpRequestMethodNotSupportedException handling")
    void testMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ApiResponseEntity<Void> entity = handler.handleMethodNotSupportedException(ex);
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(405, response.status());
        assertFalse(response.success());
        assertTrue(response.message().contains("DELETE"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test generic fallback Exception handling")
    void testGenericException() {
        Exception ex = new RuntimeException("Unexpected DB timeout");
        ApiResponseEntity<Void> entity = handler.handleGlobalException(ex);
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(500, response.status());
        assertFalse(response.success());
        assertEquals("服务器内部故障，请联系管理员", response.message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
    }

    @Test
    @DisplayName("Test MaintenanceModeException handling")
    void testMaintenanceModeException() {
        com.humolix.volur.framework.common.exception.MaintenanceModeException ex =
                new com.humolix.volur.framework.common.exception.MaintenanceModeException("全站维护测试公告");
        ApiResponseEntity<Void> entity = handler.handleMaintenanceModeException(ex);
        ApiResponse<Void> response = entity.getBody();

        assertNotNull(response);
        assertEquals(503, response.status());
        assertFalse(response.success());
        assertEquals("全站维护测试公告", response.message());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, entity.getStatusCode());
    }

}
