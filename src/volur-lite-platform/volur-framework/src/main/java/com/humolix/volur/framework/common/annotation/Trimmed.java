package com.humolix.volur.framework.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.humolix.volur.framework.common.jackson.StringTrimmingDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = StringTrimmingDeserializer.class)
public @interface Trimmed {

    boolean toLowerCase() default false;

    boolean emptyToNull() default false;

}
