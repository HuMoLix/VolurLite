package com.humolix.volur.security.domain.session;

import com.humolix.volur.framework.common.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHeader implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userAgent;

    private String accept;

    private String acceptLanguage;

    private String acceptEncoding;

    private String secChUa;

    private String secChUaMobile;

    private String secChUaPlatform;

    private String secChUaPlatformVersion;

    private String origin;

    private String referer;

    private String secFetchSite;

    private String secFetchMode;

    private String secFetchDest;

    private String xForwardedFor;

    private String xRealIp;

    private String xRequestedWith;

    private String host;

    private String connection;

    private String cacheControl;

    private String upgradeInsecureRequests;

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    public static RequestHeader from(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Map<String, String> rawHeaders = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                if (StringUtils.hasText(name) && value != null) {
                    rawHeaders.put(name.toLowerCase(), value);
                }
            }
        }

        return RequestHeader.builder()
                .userAgent(request.getHeader("User-Agent"))
                .accept(request.getHeader("Accept"))
                .acceptLanguage(request.getHeader("Accept-Language"))
                .acceptEncoding(request.getHeader("Accept-Encoding"))
                .secChUa(request.getHeader("Sec-CH-UA"))
                .secChUaMobile(request.getHeader("Sec-CH-UA-Mobile"))
                .secChUaPlatform(request.getHeader("Sec-CH-UA-Platform"))
                .secChUaPlatformVersion(request.getHeader("Sec-CH-UA-Platform-Version"))
                .origin(request.getHeader("Origin"))
                .referer(request.getHeader("Referer"))
                .secFetchSite(request.getHeader("Sec-Fetch-Site"))
                .secFetchMode(request.getHeader("Sec-Fetch-Mode"))
                .secFetchDest(request.getHeader("Sec-Fetch-Dest"))
                .xForwardedFor(request.getHeader("X-Forwarded-For"))
                .xRealIp(request.getHeader("X-Real-IP"))
                .xRequestedWith(request.getHeader("X-Requested-With"))
                .host(request.getHeader("Host"))
                .connection(request.getHeader("Connection"))
                .cacheControl(request.getHeader("Cache-Control"))
                .upgradeInsecureRequests(request.getHeader("Upgrade-Insecure-Requests"))
                .headers(rawHeaders)
                .build();
    }

    public static RequestHeader fromCurrentRequest() {
        try {
            return from(RequestUtil.getCurrentRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static RequestHeader fromUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        Map<String, String> rawHeaders = new HashMap<>();
        rawHeaders.put("user-agent", userAgent);
        return RequestHeader.builder()
                .userAgent(userAgent)
                .headers(rawHeaders)
                .build();
    }

    public String getHeader(String name) {
        if (!StringUtils.hasText(name) || headers == null) {
            return null;
        }
        return headers.get(name.toLowerCase());
    }

}
