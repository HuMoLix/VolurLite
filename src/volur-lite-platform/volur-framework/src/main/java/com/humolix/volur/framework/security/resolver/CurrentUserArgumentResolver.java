package com.humolix.volur.framework.security.resolver;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.annotation.CurrentUser;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation == null || annotation.required();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            if (required) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
            }
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal != null && parameter.getParameterType().isInstance(principal)) {
            return principal;
        }

        if (required) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "当前用户主体类型不匹配");
        }
        return null;
    }

}
