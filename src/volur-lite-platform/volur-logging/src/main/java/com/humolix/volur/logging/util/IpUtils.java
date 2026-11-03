package com.humolix.volur.logging.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return LOCALHOST_IPV4;
        }

        String ip = request.getHeader("X-Real-IP");
        if (!isValidIp(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (!isValidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!isValidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!isValidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (!isValidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        if (StringUtils.hasText(ip)) {
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            if (LOCALHOST_IPV6.equals(ip)) {
                ip = LOCALHOST_IPV4;
            }
            return ip;
        }

        return LOCALHOST_IPV4;
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip.trim());
    }

}
