package com.humolix.volur.auth.security.jwt;

import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenMalformedException;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenMissingException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private SystemSettingService systemSettingService;

    private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        lenient().when(systemSettingService.getLong(eq(SystemSettingKeys.AUTH_JWT_EXPIRE_SECONDS), anyLong())).thenReturn(3600L);
        lenient().when(systemSettingService.getLong(eq(SystemSettingKeys.AUTH_JWT_REFRESH_EXPIRATION_MS), anyLong())).thenReturn(604800000L);
        lenient().when(systemSettingService.getLong(eq(SystemSettingKeys.AUTH_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS), anyLong())).thenReturn(2592000000L);

        jwtUtil = new JwtUtil(systemSettingService);

        String rawSecret = "12345678901234567890123456789012";
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        ReflectionTestUtils.setField(jwtUtil, "secretKeyStr", base64Secret);

        jwtUtil.init();

        testUser = new User();
        testUser.setId(1001L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.USER);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateAccessTokenFromUser(testUser, jti);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateTokenWithoutBlacklist(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("testuser");
        assertThat(jwtUtil.getUserIdFromTokenAsLong(token)).isEqualTo(1001L);
        assertThat(jwtUtil.getJtiFromToken(token)).isEqualTo(jti);
        assertThat(jwtUtil.getRolesFromToken(token)).contains("ROLE_USER");

        Jwt jwt = jwtUtil.getClaimsFromToken(token);
        assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        assertThat(jwtUtil.getExpirationTimeFromToken(token)).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshTokenFromUser(testUser, jti, true);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtil.validateRefreshToken(refreshToken)).isTrue();

        Jwt jwt = jwtUtil.getClaimsFromToken(refreshToken);
        assertThat(jwt.getClaimAsString("type")).isEqualTo("refresh");
        assertThat(jwt.getClaimAsBoolean("rememberMe")).isTrue();
    }

    @Test
    void shouldThrowTokenMalformedExceptionForTamperedToken() {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateAccessTokenFromUser(testUser, jti);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> jwtUtil.getClaimsFromToken(tamperedToken))
                .isInstanceOf(TokenMalformedException.class);
        assertThat(jwtUtil.validateTokenWithoutBlacklist(tamperedToken)).isFalse();
    }

    @Test
    void shouldParseTokenFromRequest() {
        MockHttpServletRequest requestWithCookie = new MockHttpServletRequest();
        requestWithCookie.setCookies(new Cookie("access_token", "sample-cookie-token"));
        assertThat(jwtUtil.parseToken(requestWithCookie)).isEqualTo("sample-cookie-token");

        MockHttpServletRequest requestWithBearer = new MockHttpServletRequest();
        requestWithBearer.addHeader("Authorization", "Bearer sample-jwt-token");
        assertThatThrownBy(() -> jwtUtil.parseToken(requestWithBearer))
                .isInstanceOf(TokenMissingException.class);

        MockHttpServletRequest requestWithQuery = new MockHttpServletRequest();
        requestWithQuery.setParameter("token", "query-jwt-token");
        assertThatThrownBy(() -> jwtUtil.parseToken(requestWithQuery))
                .isInstanceOf(TokenMissingException.class);

        MockHttpServletRequest requestWithAccessToken = new MockHttpServletRequest();
        requestWithAccessToken.setParameter("access_token", "param-access-token");
        assertThatThrownBy(() -> jwtUtil.parseToken(requestWithAccessToken))
                .isInstanceOf(TokenMissingException.class);

        MockHttpServletRequest emptyRequest = new MockHttpServletRequest();
        assertThatThrownBy(() -> jwtUtil.parseToken(emptyRequest))
                .isInstanceOf(TokenMissingException.class);
    }
}
