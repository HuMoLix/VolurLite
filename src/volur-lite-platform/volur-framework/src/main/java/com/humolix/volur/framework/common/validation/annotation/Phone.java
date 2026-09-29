package com.humolix.volur.framework.common.validation.annotation;

import com.humolix.volur.framework.common.validation.enums.ChinaCarrier;
import com.humolix.volur.framework.common.validation.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 中国大陆手机号码格式校验注解
 * <p>
 * 遵循 Jakarta Bean Validation 标准规范，支持在实体字段、方法参数以及方法返回值上进行校验。
 * 现阶段严格限定为中国大陆四大基础电信业务经营商（中国移动、中国联通、中国电信、中国广电）的合规 11 位分配号段。
 * 默认支持四大运营商全部号段，亦可通过 {@link #carriers()} 精确限定单个或多个特定运营商。
 * </p>
 */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    /**
     * 校验失败时的提示信息
     */
    String message() default "手机号码格式不正确";

    /**
     * 约束所属的分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 允许匹配的运营商范围，默认为全部四大运营商
     */
    ChinaCarrier[] carriers() default {ChinaCarrier.ALL};
}
