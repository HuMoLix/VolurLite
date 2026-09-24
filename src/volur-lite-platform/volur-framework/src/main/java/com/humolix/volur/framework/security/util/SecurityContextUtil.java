package com.humolix.volur.framework.security.util;

import com.humolix.volur.framework.security.CurrentUserSchema;
import com.humolix.volur.framework.security.UserIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 全局安全与请求执行上下文工具类
 * <p>
 * 提供请求范围内的用户身份与实时网络环境快照获取功能，作为全系统用户上下文统一门面。
 * </p>
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 获取当前执行上下文的用户快照 Schema (永不为 null)
     *
     * @return 包含实时 IP、身份 ID、用户名、认证状态及角色列表的上下文快照
     */
    public static CurrentUserSchema getCurrentUserSchema() {
        String remoteIp = RequestUtil.getRemoteIp();

        Authentication authentication;
        try {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        } catch (Exception e) {
            authentication = null;
        }

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return CurrentUserSchema.builder()
                    .id(0L)
                    .username("anonymous")
                    .ip(remoteIp)
                    .authenticated(false)
                    .roles(Collections.emptyList())
                    .build();
        }

        Object principal = authentication.getPrincipal();
        Long userId = null;
        String username = authentication.getName();

        if (principal instanceof UserIdentity userIdentity) {
            userId = userIdentity.getUserId();
            username = userIdentity.getUsername();
        } else if (principal != null) {
            try {
                Method method = principal.getClass().getMethod("getUserId");
                Object result = method.invoke(principal);
                if (result instanceof Long id) {
                    userId = id;
                }
            } catch (Exception ignored) {
            }
        }

        authentication.getAuthorities();
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return CurrentUserSchema.builder()
                .id(userId != null ? userId : 0L)
                .username(username != null ? username : "anonymous")
                .ip(remoteIp)
                .authenticated(true)
                .roles(roles)
                .build();
    }

    public static Long getCurrentUserId() {
        return getCurrentUserSchema().getId();
    }

    public static String getCurrentIp() {
        return getCurrentUserSchema().getIp();
    }

    public static boolean isAuthenticated() {
        return getCurrentUserSchema().isAuthenticated();
    }

}
