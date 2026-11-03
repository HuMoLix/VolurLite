package com.humolix.volur.framework.common.validation.annotation;

import com.humolix.volur.framework.common.validation.validator.EmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * 平台通用增强型邮箱校验注解
 * <p>
 * 遵循 Jakarta Bean Validation 标准规范，继承并组合了标准 {@link jakarta.validation.constraints.Email}。
 * <ul>
 *     <li>底层基于 {@link jakarta.validation.constraints.Email} 实现标准 RFC 邮箱格式校验</li>
 *     <li>融合系统邮箱域名白名单校验机制 (基于系统配置 {@code system.auth.email_whitelist_suffixes})</li>
 *     <li>支持在实体字段、方法参数以及方法返回值上进行统一校验</li>
 * </ul>
 * </p>
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Email.List.class)
@jakarta.validation.constraints.Email
public @interface Email {

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "message")
    String message() default "邮箱格式不正确";

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "groups")
    Class<?>[] groups() default {};

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "payload")
    Class<? extends Payload>[] payload() default {};

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "regexp")
    String regexp() default ".*";

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "flags")
    Pattern.Flag[] flags() default {};

    boolean checkWhitelist() default true;

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        Email[] value();
    }
}
