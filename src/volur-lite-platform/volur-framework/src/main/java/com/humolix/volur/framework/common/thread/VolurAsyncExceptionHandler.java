package com.humolix.volur.framework.common.thread;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VolurAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "pwd", "secret", "token", "credential", "auth", "key"
    );

    @Override
    public void handleUncaughtException(@NonNull Throwable throwable, Method method, Object @NonNull ... params) {
        String declaringClass = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String sanitizedParams = sanitizeParameters(params);

        log.error("[Volur-Async] 异步任务执行异常 -> 目标类: [{}], 方法: [{}], 入参: [{}], 异常类型: [{}], 错误信息: {}",
                declaringClass,
                methodName,
                sanitizedParams,
                throwable.getClass().getName(),
                throwable.getMessage(),
                throwable
        );
    }

    private String sanitizeParameters(Object... params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        return Arrays.stream(params)
                .map(param -> {
                    if (param == null) {
                        return "null";
                    }
                    String typeName = param.getClass().getSimpleName();
                    String paramStr = param.toString();
                    boolean isSensitive = SENSITIVE_KEYWORDS.stream()
                            .anyMatch(kw -> typeName.toLowerCase().contains(kw) || paramStr.toLowerCase().contains(kw));
                    if (isSensitive) {
                        return typeName + "[PROTECTED_SENSITIVE_DATA]";
                    }
                    if (paramStr.length() > 200) {
                        return paramStr.substring(0, 200) + "...(truncated)";
                    }
                    return paramStr;
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

}
