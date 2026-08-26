package com.humolix.volur.auth.security.util;

import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.auth.security.entity.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    public static UserDetails getPrincipal() {
        return Optional.ofNullable(getPrincipalSafe())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
    }

    public static UserDetails getPrincipalSafe() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }
        return null;
    }

    public static String getCurrentUsername() {
        return getPrincipal().getUsername();
    }

    public static String getCurrentUsernameSafe() {
        UserDetails principal = getPrincipalSafe();
        return principal != null ? principal.getUsername() : null;
    }

    public static User getCurrentUser() {
        return getPrincipal().getUser();
    }

    public static User getCurrentUserSafe() {
        UserDetails principal = getPrincipalSafe();
        return principal != null ? principal.getUser() : null;
    }

    public static Long getCurrentUserId() {
        return getPrincipal().getUserId();
    }

    public static Long getCurrentUserIdSafe() {
        UserDetails principal = getPrincipalSafe();
        return principal != null ? principal.getUserId() : null;
    }

    public static boolean hasRole(String role) {
        if (role == null) return false;
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        Authentication authentication = getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), targetRole));
    }

    public static boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(SecurityUtils::hasRole);
    }

    public static boolean isSuperAdmin() {
        return hasAnyRole("SUPER_ADMIN", "SUPERADMIN");
    }

    public static boolean isAdmin() {
        return hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPERADMIN");
    }

    public static void logout() {
        SecurityContextHolder.clearContext();
    }

}
