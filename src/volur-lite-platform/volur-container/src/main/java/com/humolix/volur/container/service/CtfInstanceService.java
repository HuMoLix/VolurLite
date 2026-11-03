package com.humolix.volur.container.service;

import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;

import java.util.Collection;
import java.util.Optional;

public interface CtfInstanceService {

    ChallengeInstanceResponseVO startInstance(Long challengeId, Long userId);

    ChallengeInstanceResponseVO startInstance(Long challengeId, Long userId, Long stageId, Long teamId, String teamToken);

    void stopInstance(Long challengeId, Long userId);

    ChallengeInstanceResponseVO extendInstance(Long challengeId, Long userId);

    Optional<CtfChallengeInstance> getInstance(Long challengeId, Long userId);

    boolean verifyFlag(Long challengeId, Long userId, String flag);

    String getAccessUrl(Long challengeId, Long userId);

    Integer getRemainingSeconds(Long challengeId, Long userId);

    ChallengeInstanceResponseVO getTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds);

    long countActiveInstancesForTeam(Long stageId, Long teamId, Collection<Long> memberUserIds);

    void stopTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds);

    ChallengeInstanceResponseVO extendTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds);

    boolean verifyTeamFlag(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds, String flag);

}
