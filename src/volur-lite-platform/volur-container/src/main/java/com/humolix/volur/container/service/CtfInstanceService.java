package com.humolix.volur.container.service;

import com.humolix.volur.container.entity.CtfChallengeInstance;
import com.humolix.volur.container.vo.CtfTrainingChallengeInstanceResponseVO;

import java.util.Optional;

public interface CtfInstanceService {

    CtfTrainingChallengeInstanceResponseVO startTrainingInstance(Long challengeId, Long userId);

    void stopTrainingInstance(Long challengeId, Long userId);

    CtfTrainingChallengeInstanceResponseVO extendTrainingInstance(Long challengeId, Long userId);

    Optional<CtfChallengeInstance> getTrainingInstance(Long challengeId, Long userId);

    boolean verifyFlag(Long challengeId, Long userId, String flag);

}
