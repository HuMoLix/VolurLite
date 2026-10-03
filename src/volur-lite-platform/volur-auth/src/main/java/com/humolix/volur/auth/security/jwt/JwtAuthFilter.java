package com.humolix.volur.auth.security.jwt;

import com.humolix.volur.auth.security.entity.UserDetails;
import com.humolix.volur.auth.security.service.UserDetailsService;
import com.humolix.volur.auth.security.util.SecurityWhitelistMatcher;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.framework.common.exception.authException.tokenException.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final SecurityWhitelistMatcher whitelistMatcher;
    private final HandlerExceptionResolver resolver;

    public JwtAuthFilter(
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            TokenService tokenService,
            SecurityWhitelistMatcher whitelistMatcher,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.whitelistMatcher = whitelistMatcher;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token;
        try {
            token = jwtUtil.parseToken(request);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isWhitelisted = whitelistMatcher.match(request);

        try {
            Jwt claims = jwtUtil.getClaimsFromToken(token);

            if (tokenService.isTokenBlacklisted(claims.getId())) {
                throw new TokenBlacklistedException();
            }

            final Long userId = jwtUtil.getUserIdFromTokenAsLong(token);
            final long iat = claims.getIssuedAt() != null ? claims.getIssuedAt().toEpochMilli() : 0L;

            if (tokenService.isUserForceLoggedOut(userId, iat)) {
                throw new UserForceLoggedOutException();
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

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            if (isWhitelisted) {
                filterChain.doFilter(request, response);
                return;
            }

            Exception exception = this.fromException(ex);
            resolver.resolveException(request, response, null, exception);
        }
    }

    private Exception fromException(Exception ex) {
        return switch (ex) {
            case TokenAuthenticationException tae -> tae;
            case JwtValidationException e -> new TokenExpiredException(e);
            case BadJwtException e -> new TokenMalformedException(e);
            case JwtException e -> new InvalidTokenException("无效的令牌", e);
            case UsernameNotFoundException e -> new InvalidTokenException("无效的令牌，用户不存在", e);
            default -> ex;
        };
    }

}
