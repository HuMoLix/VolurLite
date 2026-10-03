package com.humolix.volur.logging.aspect;

import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.logging.service.SyslogPersistenceService;
import com.humolix.volur.logging.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class SyslogAspect {

    private final SyslogPersistenceService syslogPersistenceService;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:volur-platform}")
    private String serviceName;

    @Around("@annotation(syslog)")
    public Object around(ProceedingJoinPoint joinPoint, Syslog syslog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String preOperator = getSafeOperator();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            handleLogRecord(joinPoint, syslog, preOperator, result, exception, durationMs);
        }
    }

    private void handleLogRecord(
            ProceedingJoinPoint joinPoint,
            Syslog syslog,
            String preOperator,
            Object result,
            Throwable exception,
            long durationMs) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            String traceId = MDC.get("traceId");
            if (!StringUtils.hasText(traceId) && request != null) {
                traceId = request.getHeader("X-Trace-Id");
            }

            String operator = resolveOperator(preOperator, joinPoint.getArgs(), request);
            String operatorIp = IpUtils.getClientIp(request);

            com.humolix.volur.logging.domain.entity.Syslog logEntity = com.humolix.volur.logging.domain.entity.Syslog.builder()
                    .traceId(traceId)
                    .serviceName(serviceName)
                    .level(exception == null ? "INFO" : "ERROR")
                    .logType(StringUtils.hasText(syslog.logType()) ? syslog.logType() : "OPERATE")
                    .module(syslog.module())
                    .action(syslog.value())
                    .operator(operator)
                    .operatorIp(operatorIp)
                    .status(exception == null)
                    .durationMs(durationMs)
                    .build();

            if (request != null) {
                logEntity.setRequestUri(request.getRequestURI());
                logEntity.setRequestMethod(request.getMethod());
                logEntity.setUserAgent(request.getHeader("User-Agent"));
            }

            if (exception != null) {
                String errorMsg = exception.getMessage();
                if (errorMsg != null && errorMsg.length() > 2000) {
                    errorMsg = errorMsg.substring(0, 2000);
                }
                logEntity.setErrorMessage(errorMsg != null ? errorMsg : exception.getClass().getName());
            }

            if (syslog.recordParams()) {
                String paramsJson = serializeArguments(joinPoint.getArgs());
                logEntity.setParams(paramsJson);
            }

            if (syslog.recordResponse() && result != null && exception == null) {
                try {
                    Object toSerialize = result instanceof org.springframework.http.HttpEntity<?> entity ? entity.getBody() : result;
                    String resultJson = objectMapper.writeValueAsString(toSerialize);
                    if (resultJson.length() > 4000) {
                        resultJson = resultJson.substring(0, 4000) + "... (truncated)";
                    }
                    logEntity.setResult(resultJson);
                } catch (Exception ignored) {
                }
            }

            syslogPersistenceService.saveLogAsync(logEntity);
        } catch (Exception e) {
            log.warn("[Syslog] 采集操作日志异常: {}", e.getMessage());
        }
    }

    private String resolveOperator(String preOperator, Object[] args, HttpServletRequest request) {
        if (StringUtils.hasText(preOperator) && !"ANONYMOUS".equalsIgnoreCase(preOperator)) {
            return preOperator;
        }
        String postOperator = getSafeOperator();
        if (StringUtils.hasText(postOperator) && !"ANONYMOUS".equalsIgnoreCase(postOperator)) {
            return postOperator;
        }
        String argOperator = extractOperatorFromArgs(args);
        if (StringUtils.hasText(argOperator)) {
            return argOperator;
        }
        String jwtOperator = extractOperatorFromJwt(request);
        if (StringUtils.hasText(jwtOperator)) {
            return jwtOperator;
        }
        return "ANONYMOUS";
    }

    private String extractOperatorFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                java.lang.reflect.Method method = arg.getClass().getMethod("getUsername");
                Object val = method.invoke(arg);
                if (val instanceof String s && StringUtils.hasText(s)) {
                    return s.trim();
                }
            } catch (Exception ignored) {
            }
            try {
                java.lang.reflect.Method method = arg.getClass().getMethod("getAccount");
                Object val = method.invoke(arg);
                if (val instanceof String s && StringUtils.hasText(s)) {
                    return s.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractOperatorFromJwt(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(decoded);
            if (node.has("sub")) {
                String sub = node.get("sub").asText();
                if (StringUtils.hasText(sub)) {
                    return sub.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getSafeOperator() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception ignored) {
        }
        return "ANONYMOUS";
    }

    private String serializeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        List<Object> filterArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof InputStream
                    || arg instanceof OutputStream) {
                continue;
            }
            filterArgs.add(arg);
        }

        if (filterArgs.isEmpty()) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(filterArgs.size() == 1 ? filterArgs.get(0) : filterArgs);
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "... (truncated)";
            }
            return json;
        } catch (Exception e) {
            return "[Serialization Error: " + e.getMessage() + "]";
        }
    }

}
