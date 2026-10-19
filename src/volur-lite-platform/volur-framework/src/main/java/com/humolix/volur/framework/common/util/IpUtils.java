package com.humolix.volur.framework.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {

    private static final String UNKNOWN = "unknown";

    public static String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            } else {
                return ip.trim();
            }
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();

    }

}
