package com.humolix.volur.challenge.domain.entity;

import com.humolix.volur.challenge.domain.enums.ChallengeStageType;

public interface Challenge {

    Long getId();

    String getName();

    Integer getDifficulty();

    String getCategoryName();

    ChallengeStageType getStageType();

}
