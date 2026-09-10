package com.humolix.volur.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "volur.security.whitelist")
public record SecurityWhitelistProperties(
        List<String> paths,
        List<String> additionalPaths,
        List<String> allPaths
) {
    public static final List<String> WHITE_LIST_PATHS = List.of(

            // Authentication and Authorization
            "/api/v1/auth/login",
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
            "/api/v1/system/settings/public",

            // Swagger and API Documentation
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**"
    );

    public SecurityWhitelistProperties(
            @DefaultValue List<String> paths,
            @DefaultValue List<String> additionalPaths
    ) {
        this(
                paths == null || paths.isEmpty() ? WHITE_LIST_PATHS : List.copyOf(paths),
                additionalPaths == null ? List.of() : List.copyOf(additionalPaths),
                computeAllPaths(paths, additionalPaths)
        );
    }

    private static List<String> computeAllPaths(List<String> paths, List<String> additionalPaths) {
        Set<String> uniquePaths = new LinkedHashSet<>();
        if (paths == null || paths.isEmpty()) {
            uniquePaths.addAll(WHITE_LIST_PATHS);
        } else {
            uniquePaths.addAll(paths);
        }

        if (additionalPaths != null) {
            uniquePaths.addAll(additionalPaths);
        }
        return List.copyOf(uniquePaths);
    }

    public String[] asArray() {
        return allPaths.toArray(String[]::new);
    }
}
