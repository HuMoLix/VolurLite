package com.humolix.volur.challenge.domain.entity;

import com.humolix.volur.challenge.domain.enums.ChallengeMode;

public interface Challenge {

    Long getId();

    String getName();

    Integer getDifficulty();

    String getCategoryName();

    ChallengeMode getMode();

}
