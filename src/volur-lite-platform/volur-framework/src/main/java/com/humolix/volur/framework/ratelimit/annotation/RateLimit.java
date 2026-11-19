package com.humolix.volur.framework.ratelimit.annotation;

import com.humolix.volur.framework.ratelimit.enums.RateLimitType;
import org.springframework.http.HttpStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RateLimits.class)
public @interface RateLimit {

    String module() default "";

    String key();

    int count() default 5;

    long window() default 60;

    RateLimitType type() default RateLimitType.SLIDING_WINDOW;

    long lockSeconds() default 900;

    String message() default "操作过于频繁，请稍后再试";

    int errorCode() default 0;

    HttpStatus status() default HttpStatus.TOO_MANY_REQUESTS;

}
