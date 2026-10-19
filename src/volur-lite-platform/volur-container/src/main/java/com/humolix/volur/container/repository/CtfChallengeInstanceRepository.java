package com.humolix.volur.container.repository;

import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CtfChallengeInstanceRepository extends JpaRepository<CtfChallengeInstance, Long>, JpaSpecificationExecutor<CtfChallengeInstance> {

    Optional<CtfChallengeInstance> findByChallengeIdAndTargetId(Long challengeId, Long targetId);

    Optional<CtfChallengeInstance> findByStageIdAndTeamIdAndChallengeId(Long stageId, Long teamId, Long challengeId);

    Optional<CtfChallengeInstance> findFirstByChallengeIdAndTargetIdInAndStatusAndExpiresAtAfter(
            Long challengeId, Collection<Long> targetIds, InstanceStartupStatus status, Instant now
    );

    Optional<CtfChallengeInstance> findFirstByChallengeIdAndTargetIdIn(Long challengeId, Collection<Long> targetIds);

    List<CtfChallengeInstance> findByTargetId(Long targetId);

    List<CtfChallengeInstance> findByExpiresAtBeforeAndStatus(Instant now, InstanceStartupStatus status);

    long countByTargetIdAndStatus(Long targetId, InstanceStartupStatus status);

    long countByStageIdAndTeamIdAndStatusAndExpiresAtAfter(Long stageId, Long teamId, InstanceStartupStatus status, Instant now);

    @Query("SELECT COUNT(DISTINCT i.id) FROM CtfChallengeInstance i WHERE i.status = :status AND i.expiresAt > :now AND ((i.stageId = :stageId AND i.teamId = :teamId) OR (i.stageId = :stageId AND i.targetId IN :targetIds))")
    long countActiveByStageAndTeamOrTargetIds(
            @Param("stageId") Long stageId,
            @Param("teamId") Long teamId,
            @Param("targetIds") Collection<Long> targetIds,
            @Param("status") InstanceStartupStatus status,
            @Param("now") Instant now
    );

    long countByStatus(InstanceStartupStatus status);

    long countByStatusIn(Collection<InstanceStartupStatus> statuses);

    List<CtfChallengeInstance> findByStatusIn(Collection<InstanceStartupStatus> statuses);

    long countByCreatedAtAfter(Instant after);

    long countByNodeIpAndStatusIn(String nodeIp, Collection<InstanceStartupStatus> statuses);

    List<CtfChallengeInstance> findByNodeIp(String nodeIp);

    void deleteByChallengeIdAndTargetId(Long challengeId, Long targetId);

}
