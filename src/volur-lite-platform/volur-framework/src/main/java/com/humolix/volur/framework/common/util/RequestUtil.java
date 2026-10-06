package com.humolix.volur.framework.common.util;

import cn.hutool.core.net.NetUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

/**
 * 全局 HTTP 请求上下文工具类
 */
public class RequestUtil {

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

    public static String getClientIp(HttpServletRequest request) {
        return resolveClientIp(request);
    }

    public static String getRemoteIp() {
        try {
            HttpServletRequest request = getCurrentRequest();
            return resolveClientIp(request);
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        try {
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
        boolean hasPublicIp = ipList.stream().anyMatch(RequestUtil::isPublicIp);

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
            return ipList.getFirst();
        }

        return ipList.getFirst();
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

    public static final String COOKIE_DEVICE_TOKEN = "_v_dtk";
    public static final String COOKIE_DEVICE_ID = "_v_did";
    public static final String COOKIE_DEVICE_FINGERPRINT = "fingerprint";

    /**
     * 获取指定 HTTP 请求中的 Cookie 对象
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        if (request == null || !StringUtils.hasText(name)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 从当前请求上下文中获取指定名称的 Cookie 对象
     */
    public static Cookie getCookie(String name) {
        try {
            return getCookie(getCurrentRequest(), name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取指定 HTTP 请求中指定名称的 Cookie 文本值 (自动安全 URL 解码与清洗)
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = getCookie(request, name);
        if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
            return null;
        }
        try {
            return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return cookie.getValue().trim();
        }
    }

    /**
     * 从当前请求上下文中获取指定名称的 Cookie 文本值
     */
    public static String getCookieValue(String name) {
        try {
            return getCookieValue(getCurrentRequest(), name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端通过 Cookie 上报的设备风控 Token 或设备指纹
     * 遵循大厂分层矩阵规范：
     * 1. 优先读取动态风控凭证 (_v_dtk);
     * 2. 兜底读取长效设备种子 (_v_did);
     * 3. 兼容通用 fingerprint Cookie.
     * 彻底废除并删除 X-Device-Fingerprint 请求头，严格以 Cookie 为唯一权威标准。
     */
    public static String getDeviceFingerprint() {
        try {
            return getDeviceFingerprint(getCurrentRequest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取指定请求中通过 Cookie 上报的设备风控 Token 或设备指纹
     */
    public static String getDeviceFingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = getCookieValue(request, COOKIE_DEVICE_TOKEN);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        String did = getCookieValue(request, COOKIE_DEVICE_ID);
        if (StringUtils.hasText(did)) {
            return did.trim();
        }
        String fp = getCookieValue(request, COOKIE_DEVICE_FINGERPRINT);
        if (StringUtils.hasText(fp)) {
            return fp.trim();
        }
        return null;
    }

    public static String getDeviceToken() {
        try {
            return getDeviceToken(getCurrentRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDeviceToken(HttpServletRequest request) {
        return getCookieValue(request, COOKIE_DEVICE_TOKEN);
    }

    public static String getDeviceId() {
        try {
            return getDeviceId(getCurrentRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDeviceId(HttpServletRequest request) {
        return getCookieValue(request, COOKIE_DEVICE_ID);
    }

    /**
     * 获取 Nginx 原生透传的 TLS 协商协议版本 (X-TLS-Protocol)
     */
    public static String getTlsProtocol() {
        return cleanHeader(getHeader("X-TLS-Protocol"));
    }

    /**
     * 获取 Nginx 原生透传的 TLS 协商加密套件 (X-TLS-Cipher)
     */
    public static String getTlsCipher() {
        return cleanHeader(getHeader("X-TLS-Cipher"));
    }

    /**
     * 获取 Nginx 原生透传的客户端支持加密套件序列 (X-TLS-Ciphers)
     */
    public static String getTlsCiphers() {
        return cleanHeader(getHeader("X-TLS-Ciphers"));
    }

    /**
     * 获取 Nginx 原生透传的客户端支持椭圆曲线序列 (X-TLS-Curves)
     */
    public static String getTlsCurves() {
        return cleanHeader(getHeader("X-TLS-Curves"));
    }

    /**
     * 获取 Nginx 原生透传的 ALPN 应用层协商协议 (X-TLS-ALPN, 如 h2 / http/1.1)
     */
    public static String getTlsAlpn() {
        return cleanHeader(getHeader("X-TLS-ALPN"));
    }

    /**
     * 获取当前请求的 Origin 请求头
     */
    public static String getOrigin() {
        return getHeader("Origin");
    }

    /**
     * 获取指定请求的 Origin 请求头
     */
    public static String getOrigin(HttpServletRequest request) {
        return request != null ? request.getHeader("Origin") : null;
    }

    /**
     * 从 Origin 完整 URL 中解析出纯 Host 主机名 (过滤协议、端口、IPv6 方括号与 "null")
     */
    public static String extractOriginHost(String origin) {
        if (!StringUtils.hasText(origin) || "null".equalsIgnoreCase(origin)) {
            return null;
        }
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            if (StringUtils.hasText(host)) {
                if (host.startsWith("[") && host.endsWith("]")) {
                    return host.substring(1, host.length() - 1);
                }
                return host;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取当前请求实际访问的目标 Host（自适应兼容 Nginx 反代透传的 X-Forwarded-Host、Host 头与 getServerName）
     */
    public static String getRequestHost() {
        try {
            return extractRequestHost(getCurrentRequest());
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 获取指定请求实际访问的目标 Host（自适应兼容 Nginx 反代透传的 X-Forwarded-Host、Host 头与 getServerName）
     */
    public static String extractRequestHost(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwardedHost)) {
            return extractHost(forwardedHost);
        }
        String hostHeader = request.getHeader("Host");
        if (StringUtils.hasText(hostHeader)) {
            return extractHost(hostHeader);
        }
        return request.getServerName();
    }

    /**
     * 清洗 Host 字符串，剔除端口号、多级代理逗号及 IPv6 方括号
     */
    public static String extractHost(String hostHeader) {
        if (!StringUtils.hasText(hostHeader)) {
            return null;
        }
        String host = hostHeader.trim();
        if (host.contains(",")) {
            host = host.split(",")[0].trim();
        }
        if (host.startsWith("[")) {
            int closingBracket = host.indexOf(']');
            if (closingBracket != -1) {
                return host.substring(1, closingBracket);
            }
        }
        int colonIdx = host.indexOf(':');
        if (colonIdx != -1) {
            return host.substring(0, colonIdx);
        }
        return host;
    }

    /**
     * 判定来源 Origin 是否合法放行（支持本地开发、生产同 Host 自适应以及追加白名单）
     *
     * @param origin 跨域请求发起方 Origin
     * @param request 当前 HTTP 请求
     * @param additionalOrigins 额外配置的受信 Origin/Host 集合
     * @return true 若允许跨域放行
     */
    public static boolean isOriginAllowed(String origin, HttpServletRequest request, Collection<String> additionalOrigins) {
        if (!StringUtils.hasText(origin) || request == null) {
            return false;
        }
        String originHost = extractOriginHost(origin);
        if (!StringUtils.hasText(originHost)) {
            return false;
        }
        // 1. 本地开发环境放行
        if ("localhost".equalsIgnoreCase(originHost)
                || "127.0.0.1".equals(originHost)
                || "::1".equals(originHost)) {
            return true;
        }
        // 2. 生产环境：Origin 的 Host 与请求实际访问的 Host 完全自适应一致时放行
        String requestHost = extractRequestHost(request);
        if (StringUtils.hasText(requestHost) && originHost.equalsIgnoreCase(requestHost)) {
            return true;
        }
        // 3. 检查额外配置的白名单
        if (additionalOrigins != null && !additionalOrigins.isEmpty()) {
            for (String allowed : additionalOrigins) {
                if (StringUtils.hasText(allowed)) {
                    String trimmed = allowed.trim();
                    if (trimmed.equalsIgnoreCase(origin) || trimmed.equalsIgnoreCase(originHost)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String cleanHeader(String val) {
        if (!StringUtils.hasText(val) || "-".equals(val.trim())) {
            return null;
        }
        return val.trim();
    }

}
