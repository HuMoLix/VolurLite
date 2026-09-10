package com.humolix.volur.auth.security.util;

import com.humolix.volur.auth.config.SecurityWhitelistProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 统一安全白名单路径匹配器
 */
@Component
@RequiredArgsConstructor
public class SecurityWhitelistMatcher {

    private final SecurityWhitelistProperties whitelistProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 校验指定 URL 路径是否匹配白名单规则（支持 Ant 通配符）
     *
     * @param path 请求 URI 或 ServletPath
     * @return true 若命中白名单
     */
    public boolean match(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        List<String> allPaths = whitelistProperties.allPaths();
        for (String pattern : allPaths) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验 HttpServletRequest 是否匹配白名单规则
     *
     * @param request HTTP 请求
     * @return true 若命中白名单
     */
    public boolean match(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String servletPath = request.getServletPath();
        if (match(servletPath)) {
            return true;
        }
        String requestUri = request.getRequestURI();
        return match(requestUri);
    }

    /**
     * 获取所有白名单规则数组，供 Spring Security HttpSecurity 注册使用
     */
    public String[] getPatterns() {
        return whitelistProperties.asArray();
    }
}
