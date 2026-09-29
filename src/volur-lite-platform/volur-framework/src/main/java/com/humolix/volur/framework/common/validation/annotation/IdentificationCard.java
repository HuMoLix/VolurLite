package com.humolix.volur.framework.common.validation.annotation;

import com.humolix.volur.framework.common.validation.validator.IdentificationCardValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 中国大陆第二代公民身份证号码校验注解 (GB 11643-1999)
 * <p>
 * 遵循 Jakarta Bean Validation 标准规范，支持在实体字段、方法参数以及方法返回值上进行校验。
 * 针对中国大陆 18 位第二代居民身份证进行严格校验，包含：
 * <ul>
 *     <li>18 位定长校验与基础正则预筛</li>
 *     <li>省级行政区划代码合法性校验 (前 2 位)</li>
 *     <li>出生日期有效性校验 (公历平闰年、天数有效性、历史区间与未来日期拦截)</li>
 *     <li>ISO 7064:1983.MOD 11-2 第 18 位校验码加权模 11 算法验证</li>
 * </ul>
 * </p>
 */
@Documented
@Constraint(validatedBy = IdentificationCardValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentificationCard {

    /**
     * 校验失败时的提示信息
     */
    String message() default "身份证号码格式不正确";

    /**
     * 约束所属的分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     */
    Class<? extends Payload>[] payload() default {};
}
