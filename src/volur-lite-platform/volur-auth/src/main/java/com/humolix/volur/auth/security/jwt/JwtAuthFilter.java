package com.humolix.volur.auth.security.jwt;

import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.auth.security.entity.UserDetails;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.auth.security.jwt.exception.InvalidTokenException;
import com.humolix.volur.auth.security.jwt.exception.MissingTokenException;
import com.humolix.volur.auth.security.service.UserDetailsService;
import com.humolix.volur.auth.security.util.SecurityWhitelistMatcher;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final SecurityWhitelistMatcher whitelistMatcher;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token;
        try {
            token = jwtUtil.parseToken(request);
        } catch (MissingTokenException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isWhitelisted = whitelistMatcher.match(request);

        try {
            Claims claims = jwtUtil.getClaimsFromToken(token);

            if (tokenService.isTokenBlacklisted(claims.getId())) {
                if (isWhitelisted) {
                    log.debug("白名单路径 [{}] 携带了已拉黑令牌，优雅降级为匿名放行", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "令牌已失效");
                return;
            }

            final Long userId = claims.get("userId", Long.class);
            final long iat = claims.getIssuedAt().getTime();

            if (tokenService.isUserForceLoggedOut(userId, iat)) {
                if (isWhitelisted) {
                    log.debug("白名单路径 [{}] 携带了被强制下线令牌，优雅降级为匿名放行", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "账号已在其他位置强制登出");
                return;
            }

            final String username = claims.getSubject();
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                final UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (ExpiredJwtException ex) {
            if (isWhitelisted) {
                log.debug("白名单路径 [{}] 携带了已过期令牌，优雅降级为匿名放行", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "令牌已过期");
            return;
        } catch (MalformedJwtException | InvalidTokenException ex) {
            if (isWhitelisted) {
                log.debug("白名单路径 [{}] 携带了格式非法令牌，优雅降级为匿名放行", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "令牌无效或签名错误");
            return;
        } catch (UsernameNotFoundException ex) {
            if (isWhitelisted) {
                filterChain.doFilter(request, response);
                return;
            }
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "用户不存在");
            return;
        } catch (Exception ex) {
            log.error("JWT 认证异常: {}", ex.getMessage());
            if (isWhitelisted) {
                filterChain.doFilter(request, response);
                return;
            }
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "认证服务异常");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {

        if (response.isCommitted()) return;
        ApiResponse<?> apiResponse = ApiResponseEntity.of(status, message, null);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }

}
