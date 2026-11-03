package com.humolix.volur.framework.id.annotation;

import com.humolix.volur.framework.id.jpa.SnowflakeIdentifierGenerator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@IdGeneratorType(SnowflakeIdentifierGenerator.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SnowflakeGenerated {

}
