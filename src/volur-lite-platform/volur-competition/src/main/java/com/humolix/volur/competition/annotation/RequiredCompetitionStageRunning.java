package com.humolix.volur.competition.annotation;

import com.humolix.volur.competition.domain.enums.CompetitionType;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredCompetitionStageRunning {

    CompetitionType value() default CompetitionType.CTF;

    CompetitionType type() default CompetitionType.CTF;

    String key() default "";

}
