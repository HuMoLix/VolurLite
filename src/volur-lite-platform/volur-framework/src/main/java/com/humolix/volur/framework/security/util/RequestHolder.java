package com.humolix.volur.framework.security.util;

import cn.hutool.core.net.NetUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
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

            // 1. 优先提取不可伪造的 CDN/WAF 高信任度真实客户端 IP (Cloudflare CF-Connecting-IP, True-Client-IP)
            String cfIp = cleanIp(request.getHeader("CF-Connecting-IP"));
            if (isValidIp(cfIp)) {
                return normalizeIp(cfIp);
            }

            String trueClientIp = cleanIp(request.getHeader("True-Client-IP"));
            if (isValidIp(trueClientIp)) {
                return normalizeIp(trueClientIp);
            }

            // 2. 检查 X-Forwarded-For 代理链路 (自右向左标准可信代理回溯)
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff) && !"unknown".equalsIgnoreCase(xff)) {
                String resolvedFromXff = resolveFromXForwardedFor(xff);
                if (StringUtils.hasText(resolvedFromXff)) {
                    return normalizeIp(resolvedFromXff);
                }
            }

            // 3. 检查 X-Real-IP
            String realIp = cleanIp(request.getHeader("X-Real-IP"));
            if (isValidIp(realIp)) {
                return normalizeIp(realIp);
            }

            // 4. 回退至底层 TCP 连接对端 IP
            return normalizeIp(request.getRemoteAddr());
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static String resolveFromXForwardedFor(String xff) {
        String[] rawIps = xff.split(",");
        java.util.List<String> ipList = new java.util.ArrayList<>(rawIps.length);
        for (String raw : rawIps) {
            String ip = cleanIp(raw);
            if (isValidIp(ip)) {
                ipList.add(ip);
            }
        }

        if (ipList.isEmpty()) {
            return null;
        }

        // 探测链路中是否存在公网 IP (判断当前是否处于公网多层反代/FRP穿透环境)
        boolean hasPublicIp = ipList.stream().anyMatch(RequestHolder::isPublicIp);

        if (hasPublicIp) {
            // 公网/FRP 穿透拓扑：从右向左回溯，跳过所有内部私网/中继代理(Docker网桥、FRP本地中继)，
            // 遇到的第一个公网 IP 即为直连公网边缘代理的真实客户端，同时天然阻断左侧伪造的欺骗 IP
            for (int i = ipList.size() - 1; i >= 0; i--) {
                String candidate = ipList.get(i);
                if (isPublicIp(candidate)) {
                    return candidate;
                }
            }
        } else {
            // 纯内网/局域网拓扑 (例如线下 CTF 比赛、私有局域网直连开箱即用，无公网 IP)：
            // 从右向左回溯，跳过容器网桥(172.16.0.0/12)和回环(127.0.0.1)，提取真实的局域网客户端机器 IP (192.168.x.x / 10.x.x.x)
            for (int i = ipList.size() - 1; i >= 0; i--) {
                String candidate = ipList.get(i);
                if (!isLoopbackOrContainerBridge(candidate)) {
                    return candidate;
                }
            }
            return ipList.get(0);
        }

        return ipList.get(0);
    }

    private static boolean isPublicIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return false;
        }
        return !NetUtil.isInnerIP(ip);
    }

    private static boolean isLoopbackOrContainerBridge(String ip) {
        if (!isValidIp(ip)) {
            return true;
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        if (ip.startsWith("169.254.")) {
            return true; // 链路本地 Link-local
        }
        // Docker 默认网桥网段 172.16.0.0/12 (172.16.x.x - 172.31.x.x)
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private static final java.util.regex.Pattern IPV4_PATTERN = java.util.regex.Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );

    private static boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        String clean = ip.trim();
        if (IPV4_PATTERN.matcher(clean).matches()) {
            return true;
        }
        return clean.contains(":") && ("::1".equals(clean) || clean.indexOf(':') != clean.lastIndexOf(':'));
    }

    private static String cleanIp(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String ip = raw.trim();
        if (ip.contains(":") && !ip.contains("::") && ip.indexOf(':') == ip.lastIndexOf(':')) {
            ip = ip.substring(0, ip.indexOf(':')).trim();
        }
        return ip;
    }

    private static String normalizeIp(String ip) {
        if (!isValidIp(ip)) {
            return "127.0.0.1";
        }
        String clean = cleanIp(ip);
        return "0:0:0:0:0:0:0:1".equals(clean) ? "127.0.0.1" : clean;
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
