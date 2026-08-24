package com.humolix.volur.challenge.service.provider;

public interface ChallengeContainerProvider {

    boolean verifyFlag(Long challengeId, Long userId, String flag);

    String getAccessUrl(Long challengeId, Long userId);

    Integer getRemainingSeconds(Long challengeId, Long userId);

}
