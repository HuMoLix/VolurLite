package com.humolix.volur.framework.common.exception.handler;

import com.humolix.volur.framework.common.api.ApiResponse;
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
        ApiResponse<Void> response = handler.handleBusinessException(ex);

        assertEquals(404, response.status());
        assertEquals(40401, response.errorCode());
        assertFalse(response.success());
        assertEquals("题目不存在", response.message());
    }

    @Test
    @DisplayName("Test BadCredentialsException handling")
    void testBadCredentialsException() {
        ApiResponse<Void> response = handler.handleBadCredentialsException();
        assertEquals(401, response.status());
        assertFalse(response.success());
        assertEquals("用户名或密码错误", response.message());
    }

    @Test
    @DisplayName("Test AccessDeniedException handling")
    void testAccessDeniedException() {
        ApiResponse<Void> response = handler.handleAccessDeniedException();
        assertEquals(403, response.status());
        assertFalse(response.success());
        assertEquals("权限不足，拒绝访问", response.message());
    }

    @Test
    @DisplayName("Test NoResourceFoundException handling")
    void testNoResourceFoundException() {
        ApiResponse<Void> response = handler.handleNoResourceFoundException();
        assertEquals(404, response.status());
        assertFalse(response.success());
        assertEquals("访问的资源不存在", response.message());
    }

    @Test
    @DisplayName("Test HttpRequestMethodNotSupportedException handling")
    void testMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ApiResponse<Void> response = handler.handleMethodNotSupportedException(ex);
        assertEquals(405, response.status());
        assertFalse(response.success());
        assertTrue(response.message().contains("DELETE"));
    }

    @Test
    @DisplayName("Test generic fallback Exception handling")
    void testGenericException() {
        Exception ex = new RuntimeException("Unexpected DB timeout");
        ApiResponse<Void> response = handler.handleGlobalException(ex);
        assertEquals(500, response.status());
        assertFalse(response.success());
        assertEquals("服务器内部故障，请联系管理员", response.message());
    }

    @Test
    @DisplayName("Test MaintenanceModeException handling")
    void testMaintenanceModeException() {
        com.humolix.volur.framework.common.exception.MaintenanceModeException ex =
                new com.humolix.volur.framework.common.exception.MaintenanceModeException("全站维护测试公告");
        ApiResponse<Void> response = handler.handleMaintenanceModeException(ex);
        assertEquals(503, response.status());
        assertFalse(response.success());
        assertEquals("全站维护测试公告", response.message());
    }

}
