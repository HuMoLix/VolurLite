package com.humolix.volur.auth.security.util;

import com.humolix.volur.auth.config.SecurityWhitelistProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityWhitelistMatcher {

    private final SecurityWhitelistProperties whitelistProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

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

    public String[] getPatterns() {
        return whitelistProperties.asArray();
    }
}
