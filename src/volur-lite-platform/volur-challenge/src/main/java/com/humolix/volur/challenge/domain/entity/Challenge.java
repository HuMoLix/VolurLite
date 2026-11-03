package com.humolix.volur.challenge.domain.entity;

import com.humolix.volur.challenge.domain.enums.ChallengeMode;

public interface Challenge {

    Long getId();

    String getName();

    default Object getDifficulty() {
        return null;
    }

    String getCategoryName();

    ChallengeMode getMode();

}
