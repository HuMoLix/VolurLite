package com.humolix.volur.framework.common.api;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Getter
@Builder
public final class ApiResponseCookie {

    private final String name;
    private final String value;

    @Builder.Default
    private final Duration maxAge = Duration.ofDays(1);

    @Builder.Default
    private final String domain = "";

    @Builder.Default
    private final String path = "/";

    @Builder.Default
    private final boolean secure = false;

    @Builder.Default
    private final boolean httpOnly = true;

    @Builder.Default
    private final String sameSite = "Lax";

    @Builder.Default
    private final boolean partitioned = false;

    public ResponseCookie toResponseCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value != null ? value : "")
                .path(path)
                .maxAge(maxAge)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite);

        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        if (partitioned) {
            builder.partitioned(true);
        }
        return builder.build();
    }

    public static ApiResponseCookie of(String name, String value, Duration maxAge) {
        return ApiResponseCookie.builder()
                .name(name)
                .value(value)
                .maxAge(maxAge)
                .build();
    }

    public static ApiResponseCookie ofHttpOnly(String name, String value, Duration maxAge, boolean secure) {
        return ApiResponseCookie.builder()
                .name(name)
                .value(value)
                .maxAge(maxAge)
                .secure(secure)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
    }

    public static ApiResponseCookie deletion(String name, String path, boolean secure) {
        return ApiResponseCookie.builder()
                .name(name)
                .value("")
                .path(path)
                .maxAge(Duration.ZERO)
                .secure(secure)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
    }
}
