package com.humolix.volur.auth.security.filter;

import com.humolix.volur.framework.common.exception.authException.csrfException.CsrfOriginDeniedException;
import com.humolix.volur.framework.common.exception.authException.csrfException.CsrfTokenMismatchException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrfProtectionFilterTest {

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private FilterChain filterChain;

    private CsrfProtectionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CsrfProtectionFilter(resolver);
    }

    @Test
    void shouldPassSafeMethods() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    void shouldPassUnauthenticatedRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    void shouldRejectDisallowedOrigin() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user/update");
        request.setCookies(new Cookie("access_token", "sample-token"), new Cookie("XSRF-TOKEN", "csrf-val"));
        request.addHeader("Origin", "https://evil.com");
        request.addHeader("Host", "app.volur.com");
        request.addHeader("X-XSRF-TOKEN", "csrf-val");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CsrfOriginDeniedException.class);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectMissingCsrfHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user/update");
        request.setCookies(new Cookie("access_token", "sample-token"), new Cookie("XSRF-TOKEN", "csrf-val"));
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Host", "localhost:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CsrfTokenMismatchException.class);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectMismatchedCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/user/password");
        request.setCookies(new Cookie("access_token", "sample-token"), new Cookie("XSRF-TOKEN", "correct-csrf"));
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Host", "localhost:8080");
        request.addHeader("X-XSRF-TOKEN", "wrong-csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CsrfTokenMismatchException.class);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldPassWhenCsrfTokensMatch() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user/update");
        request.setCookies(new Cookie("access_token", "sample-token"), new Cookie("XSRF-TOKEN", "valid-csrf"));
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Host", "localhost:8080");
        request.addHeader("X-XSRF-TOKEN", "valid-csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

    @Test
    void shouldPassWithAlternativeHeaderAndAdditionalOrigins() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "additionalAllowedOrigins", List.of("https://trusted.corp.com"));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/user/avatar");
        request.setCookies(new Cookie("access_token", "sample-token"), new Cookie("XSRF-TOKEN", "token-xyz"));
        request.addHeader("Origin", "https://trusted.corp.com");
        request.addHeader("Host", "api.volur.com");
        request.addHeader("X-CSRF-TOKEN", "token-xyz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(resolver);
    }

}
