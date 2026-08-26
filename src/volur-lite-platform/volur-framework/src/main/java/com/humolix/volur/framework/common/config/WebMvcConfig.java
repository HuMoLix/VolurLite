package com.humolix.volur.framework.common.config;

import com.humolix.volur.framework.security.resolver.CurrentUserArgumentResolver;
import com.humolix.volur.framework.security.resolver.CurrentUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 基础通用配置，注册上下文身份参数解析器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdArgumentResolver());
        resolvers.add(new CurrentUserArgumentResolver());
    }

}
