package com.humolix.volur.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "volur.security.whitelist")
public record SecurityWhitelistProperties(
        @DefaultValue List<String> paths,
        @DefaultValue List<String> additionalPaths
) {
    public static final List<String> WHITE_LIST_PATHS = List.of(

            // Authentication and Authorization
            "/api/v1/auth/login",
            "/api/v1/auth/signin/**",
            "/api/v1/auth/signup/**",
            "/api/v1/auth/recovery/**",
            "/api/v1/session/**",
            "/api/v1/auth/register",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/password-resets",
            "/api/v1/auth/reset-password-confirm",
            "/api/v1/auth/refresh",

            // Verification and Notification
            "/api/v1/captcha/**",
            "/api/v1/email/**",
            "/api/v1/verification/**",

            // Public Resources
            "/api/v1/rank/**",
            "/api/v1/broadcasts/**",
            "/api/v1/competitions",
            "/api/v1/competitions/**",

            // Stream and WebSocket
            "/api/v1/ws/**",
            "/ws/**",
            "/api/v1/terminal/**",
            "/api/v1/realtime/stream",

            // Static Resources
            "/error",
            "/",
            "/index.html",
            "/favicon.ico",
            "/static/**",
            "/api/v1/uploads/**",
            "/api/v1/system/settings/public"
    );

    public SecurityWhitelistProperties {
        if (paths == null || paths.isEmpty()) {
            paths = WHITE_LIST_PATHS;
        } else {
            paths = List.copyOf(paths);
        }

        if (additionalPaths == null) {
            additionalPaths = List.of();
        } else {
            additionalPaths = List.copyOf(additionalPaths);
        }
    }

    public List<String> allPaths() {
        Set<String> uniquePaths = new LinkedHashSet<>(paths);
        uniquePaths.addAll(additionalPaths);
        return List.copyOf(uniquePaths);
    }

    public String[] asArray() {
        return allPaths().toArray(String[]::new);
    }
}
