package com.humolix.volur.competition.annotation;

import com.humolix.volur.competition.domain.enums.CompetitionStage;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredCompetitionStageRunning {

    CompetitionStage stage() default CompetitionStage.CTF;

    String key() default "";

}
