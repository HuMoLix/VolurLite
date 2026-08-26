package com.humolix.volur.auth.security.jwt;

import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.auth.security.entity.UserDetails;
import com.humolix.volur.auth.security.jwt.exception.InvalidTokenException;
import com.humolix.volur.auth.security.jwt.exception.MissingTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String REMEMBER_ME_CLAIM = "rememberMe";

    private final TokenService tokenService;

    @Value("${volur.jwt.secret:VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!}")
    private String secretKeyStr;

    @Value("${volur.jwt.expire-seconds:86400}")
    private Long expireSeconds;

    @Value("${volur.jwt.refresh-expiration-ms:604800000}")
    private Long refreshTokenExpirationMs;

    @Value("${volur.jwt.remember-me-refresh-expiration-ms:2592000000}")
    private Long rememberMeRefreshTokenExpirationMs;

    private SecretKey cachedSecretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secretKeyStr)) {
            throw new IllegalStateException("JWT Secret 必须配置且不能为空");
        }
        
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKeyStr);
        } catch (Exception e) {
            keyBytes = secretKeyStr.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        this.cachedSecretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(cachedSecretKey)
                .build();
    }

    private SecretKey getSecretKey() {
        return cachedSecretKey;
    }

    public String generateAccessToken(Authentication authentication, String jti) {
        final Instant now = Instant.now();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        long accessExpMs = expireSeconds * 1000L;
        return Jwts.builder()
                .subject(authentication.getName())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpMs)))
                .claim(AUTHORITIES_CLAIM, roles)
                .claim(USER_ID_CLAIM, Objects.requireNonNull(userDetails).getUserId())
                .claim(TOKEN_TYPE_CLAIM, "access")
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication, String jti) {
        return generateRefreshToken(authentication, jti, false);
    }

    public String generateRefreshToken(Authentication authentication, String jti, boolean rememberMe) {
        final Instant now = Instant.now();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        long expirationMs = rememberMe ? rememberMeRefreshTokenExpirationMs : refreshTokenExpirationMs;

        return Jwts.builder()
                .subject(authentication.getName())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim(USER_ID_CLAIM, Objects.requireNonNull(userDetails).getUserId())
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .claim(REMEMBER_ME_CLAIM, rememberMe)
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
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
        long accessExpMs = expireSeconds * 1000L;

        return Jwts.builder()
                .subject(user.getUsername())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpMs)))
                .claim(AUTHORITIES_CLAIM, roles)
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, "access")
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshTokenFromUser(User user, String jti) {
        return generateRefreshTokenFromUser(user, jti, false);
    }

    public String generateRefreshTokenFromUser(User user, String jti, boolean rememberMe) {
        final Instant now = Instant.now();
        long expirationMs = rememberMe ? rememberMeRefreshTokenExpirationMs : refreshTokenExpirationMs;

        return Jwts.builder()
                .subject(user.getUsername())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .claim(REMEMBER_ME_CLAIM, rememberMe)
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT 解析失败: {}", e.getMessage());
            throw new InvalidTokenException("无效的令牌: " + e.getMessage());
        }
    }

    public String getJtiFromToken(String token) {
        return getClaimsFromToken(token).getId();
    }

    public Long getIssuedAtFromToken(String token) {
        return getClaimsFromToken(token).getIssuedAt().getTime();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (tokenService.isTokenBlacklisted(claims.getId())) {
                return false;
            }
            return "refresh".equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateTokenWithBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = getClaimsFromToken(token);
            return !tokenService.isTokenBlacklisted(claims.getId());
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
        return getClaimsFromToken(token).getExpiration().getTime();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Long getUserIdFromTokenAsLong(String token) {
        return getClaimsFromToken(token).get(USER_ID_CLAIM, Long.class);
    }

    public String parseToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new MissingTokenException("未发现 Bearer 认证头部");
    }

    public String parseTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new MissingTokenException("无效的认证头部格式");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaimsFromToken(token).get(AUTHORITIES_CLAIM, List.class);
    }

}
