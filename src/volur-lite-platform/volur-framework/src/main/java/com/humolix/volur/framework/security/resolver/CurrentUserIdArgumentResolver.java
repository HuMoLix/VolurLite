package com.humolix.volur.framework.security.resolver;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.UserIdentity;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;

public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
                (Long.class.isAssignableFrom(parameter.getParameterType()) ||
                 long.class.isAssignableFrom(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation == null || annotation.required();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            if (required) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
            }
            return null;
        }

        Long userId = getUserId(authentication);
        if (userId == null || userId <= 0) {
            if (required) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
            }
            return null;
        }

        return userId;
    }

    private static @Nullable Long getUserId(Authentication authentication) {
        Long userId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserIdentity userIdentity) {
            userId = userIdentity.getUserId();
        } else if (principal != null) {
            try {
                Method method = principal.getClass().getMethod("getUserId");
                Object result = method.invoke(principal);
                if (result instanceof Long id) {
                    userId = id;
                }
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            try {
                userId = Long.parseLong(authentication.getName());
            } catch (Exception ignored) {
            }
        }
        return userId;
    }

}
