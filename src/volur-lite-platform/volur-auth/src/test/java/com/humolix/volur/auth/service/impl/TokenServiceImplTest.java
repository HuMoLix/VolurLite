package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.security.cookie.AuthCookieService;
import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private Jwt mockJwt;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(redisTemplate, jwtUtil, userRepository, authCookieService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void refreshToken_missingCookie_shouldThrowUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        assertThatThrownBy(() -> tokenService.refreshToken())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refreshToken_success_shouldWriteAuthCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.COOKIE_REFRESH_TOKEN, "valid_refresh_token"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        given(jwtUtil.validateRefreshToken("valid_refresh_token")).willReturn(true);
        given(jwtUtil.getClaimsFromToken("valid_refresh_token")).willReturn(mockJwt);
        given(mockJwt.getId()).willReturn("old_jti");
        given(redisTemplate.hasKey("vredis:auth:token:blacklist:old_jti")).willReturn(false);
        given(mockJwt.getSubject()).willReturn("testuser");

        User user = new User();
        user.setId(1001L);
        user.setUsername("testuser");
        user.setStatus(true);

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(mockJwt.getIssuedAt()).willReturn(Instant.now());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("vredis:auth:user:logout:1001")).willReturn(null);
        given(mockJwt.getClaimAsBoolean("rememberMe")).willReturn(false);
        given(mockJwt.getExpiresAt()).willReturn(Instant.now().plusSeconds(3600));

        given(jwtUtil.generateAccessTokenFromUser(eq(user), anyString())).willReturn("new_access_token");
        given(jwtUtil.generateRefreshTokenFromUser(eq(user), anyString(), eq(false))).willReturn("new_refresh_token");

        tokenService.refreshToken();

        verify(authCookieService).writeAuthCookies("new_access_token", "new_refresh_token", false);
    }

    @Test
    void logout_withAccessToken_shouldBlacklistAndClearCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.COOKIE_ACCESS_TOKEN, "valid_access_token"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        given(jwtUtil.getClaimsFromToken("valid_access_token")).willReturn(mockJwt);
        given(mockJwt.getId()).willReturn("access_jti");
        given(redisTemplate.hasKey("vredis:auth:token:blacklist:access_jti")).willReturn(false);
        given(mockJwt.getExpiresAt()).willReturn(Instant.now().plusSeconds(1800));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        tokenService.logout();

        verify(authCookieService).clearAuthCookies();
    }
}
