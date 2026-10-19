package com.humolix.volur.auth.security.jwt;

import com.humolix.volur.auth.security.entity.UserDetails;
import com.humolix.volur.framework.common.exception.authException.tokenException.InvalidTokenException;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenExpiredException;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenMalformedException;
import com.humolix.volur.framework.common.exception.authException.tokenException.TokenMissingException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String REMEMBER_ME_CLAIM = "rememberMe";

    private final SystemSettingService systemSettingService;

    @Value("${volur.jwt.secret}")
    private String secretKeyStr;

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;

    public Long getExpireSeconds() {
        if (systemSettingService == null) {
            return 86400L;
        }
        return systemSettingService.getLong(SystemSettingKeys.AUTH_JWT_EXPIRE_SECONDS, 86400L);
    }

    public Long getRefreshTokenExpirationMs() {
        if (systemSettingService == null) {
            return 604800000L;
        }
        return systemSettingService.getLong(SystemSettingKeys.AUTH_JWT_REFRESH_EXPIRATION_MS, 604800000L);
    }

    public Long getRememberMeRefreshTokenExpirationMs() {
        if (systemSettingService == null) {
            return 2592000000L;
        }
        return systemSettingService.getLong(SystemSettingKeys.AUTH_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS, 2592000000L);
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secretKeyStr)) {
            throw new IllegalStateException("Jwt Secret must be configured and not empty");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKeyStr);
        } catch (Exception e) {
            keyBytes = secretKeyStr.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        SecretKey cachedSecretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.jwtEncoder = NimbusJwtEncoder.withSecretKey(cachedSecretKey).algorithm(MacAlgorithm.HS256).build();
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(cachedSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String generateAccessToken(Authentication authentication, String jti) {
        final Instant now = Instant.now();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        long accessExpMs = getExpireSeconds() * 1000L;
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessExpMs))
                .claim(AUTHORITIES_CLAIM, roles)
                .claim(USER_ID_CLAIM, Objects.requireNonNull(userDetails).getUserId())
                .claim(TOKEN_TYPE_CLAIM, "access")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public String generateRefreshToken(Authentication authentication, String jti) {
        return generateRefreshToken(authentication, jti, false);
    }

    public String generateRefreshToken(Authentication authentication, String jti, boolean rememberMe) {
        final Instant now = Instant.now();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        long expirationMs = rememberMe ? getRememberMeRefreshTokenExpirationMs() : getRefreshTokenExpirationMs();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .claim(USER_ID_CLAIM, Objects.requireNonNull(userDetails).getUserId())
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .claim(REMEMBER_ME_CLAIM, rememberMe)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public String generateAccessTokenFromUser(User user) {
        return generateAccessTokenFromUser(user, java.util.UUID.randomUUID().toString());
    }

    public String generateAccessTokenFromUser(User user, String jti) {
        final Instant now = Instant.now();
        UserDetails userDetails = new UserDetails(user);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        long accessExpMs = getExpireSeconds() * 1000L;

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessExpMs))
                .claim(AUTHORITIES_CLAIM, roles)
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, "access")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public String generateRefreshTokenFromUser(User user, String jti) {
        return generateRefreshTokenFromUser(user, jti, false);
    }

    public String generateRefreshTokenFromUser(User user, String jti, boolean rememberMe) {
        final Instant now = Instant.now();
        long expirationMs = rememberMe ? getRememberMeRefreshTokenExpirationMs() : getRefreshTokenExpirationMs();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .claim(REMEMBER_ME_CLAIM, rememberMe)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public Jwt getClaimsFromToken(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtValidationException e) {
            log.error("JWT 验证失败 (已过期或校验失败): {}", e.getMessage());
            throw new TokenExpiredException(e);
        } catch (BadJwtException e) {
            log.error("JWT 格式或签名错误: {}", e.getMessage());
            throw new TokenMalformedException(e);
        } catch (JwtException e) {
            log.error("JWT 解析异常: {}", e.getMessage());
            throw new InvalidTokenException("无效的令牌: " + e.getMessage(), e);
        }
    }

    public String getJtiFromToken(String token) {
        return getClaimsFromToken(token).getId();
    }

    public Long getIssuedAtFromToken(String token) {
        Instant issuedAt = getClaimsFromToken(token).getIssuedAt();
        return issuedAt != null ? issuedAt.toEpochMilli() : 0L;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwt jwt = getClaimsFromToken(token);
            return "refresh".equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateTokenWithoutBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationTimeFromToken(String token) {
        Instant expiresAt = getClaimsFromToken(token).getExpiresAt();
        return expiresAt != null ? expiresAt.toEpochMilli() : 0L;
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Long getUserIdFromTokenAsLong(String token) {
        Object userId = getClaimsFromToken(token).getClaim(USER_ID_CLAIM);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId != null) {
            return Long.valueOf(userId.toString());
        }
        return null;
    }

    public String parseToken(HttpServletRequest request) {
        String token = RequestUtil.getCookieValue(request, "access_token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        throw new TokenMissingException();
    }

    public List<String> getRolesFromToken(String token) {
        return getClaimsFromToken(token).getClaimAsStringList(AUTHORITIES_CLAIM);
    }

}
