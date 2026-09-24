package com.humolix.volur.framework.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.humolix.volur.framework.common.jackson.StringTrimmingDeserializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字符串入参自动规范化与去空格声明式注解
 * <p>
 * 挂载在 Jackson 反序列化管道上，在 HTTP 请求体 JSON 绑定到 DTO 属性时高效就地清洗。
 * 支持去除首尾空格、转换为全小写 (专用于邮箱、用户名等)，以及空串转 null 控制。
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = StringTrimmingDeserializer.class)
public @interface Trimmed {

    /**
     * 是否在去除首尾空格后转换为全小写 (专用于邮箱、用户名、域名等大小写不敏感字段)
     */
    boolean toLowerCase() default false;

    /**
     * 清洗后若为空字符串，是否自动转为 null (默认 false，保持空字符串以配合 @NotBlank 触发校验)
     */
    boolean emptyToNull() default false;

}
