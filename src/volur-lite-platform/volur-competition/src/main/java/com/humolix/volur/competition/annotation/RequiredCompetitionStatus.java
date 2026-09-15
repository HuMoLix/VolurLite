package com.humolix.volur.competition.annotation;

import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredCompetitionStatus {

    CompetitionStatus[] allowed() default {CompetitionStatus.RUNNING};

    CompetitionType[] types() default {};

    String key() default "";

}
