package com.humolix.volur.framework.common.thread;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class AsyncContextTaskDecorator implements TaskDecorator {

    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {

        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        SecurityContext callerSecurityContext = SecurityContextHolder.getContext();

        return () -> {

            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            SecurityContext originalSecurityContext = SecurityContextHolder.getContext();

            try {
                if (callerMdc != null) {
                    MDC.setContextMap(callerMdc);
                } else {
                    MDC.clear();
                }

                if (MDC.get(MDC_TRACE_ID_KEY) == null) {
                    MDC.put(MDC_TRACE_ID_KEY, generateTraceId());
                }

                if (callerSecurityContext != null && callerSecurityContext.getAuthentication() != null) {
                    SecurityContextHolder.setContext(callerSecurityContext);
                }

                runnable.run();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }

                if (originalSecurityContext != null) {
                    SecurityContextHolder.setContext(originalSecurityContext);
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
