package com.humolix.volur.framework.security.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class RequestHolder {

    public static HttpServletRequest getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElseThrow(() -> new IllegalStateException("无法获取当前请求，请确保在 HTTP 请求上下文中调用"));
    }

    public static String getOriginalRequestUri() {
        try {
            HttpServletRequest request = getCurrentRequest();
            String originalUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
            return originalUri != null ? originalUri : request.getRequestURI();
        } catch (Exception e) {
            return "/";
        }
    }

    public static String getClientIp() {
        return getRemoteIp();
    }

    public static String getRemoteIp() {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    if (ip.contains(",")) {
                        String[] ips = ip.split(",");
                        ip = ips[ips.length - 1].trim();
                    }
                }
            }

            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String getUserAgent() {
        try {
            return getHeader("User-Agent");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static String getReferer() {
        return getHeader("Referer");
    }

    public static String getRequestMethod() {
        return getCurrentRequest().getMethod();
    }

    public static String getHeader(String name) {
        try {
            return getCurrentRequest().getHeader(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAjaxRequest() {
        HttpServletRequest request = getCurrentRequest();
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }

    public static String getFullRequestUrl() {
        HttpServletRequest request = getCurrentRequest();
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestURL.append('?').append(queryString);
        }
        return requestURL.toString();
    }

    public static String getDomainUrl() {
        HttpServletRequest request = getCurrentRequest();
        String url = request.getRequestURL().toString();
        String uri = request.getRequestURI();
        return url.substring(0, url.length() - uri.length());
    }

    public static boolean isJsonRequest() {
        String contentType = getCurrentRequest().getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

}
