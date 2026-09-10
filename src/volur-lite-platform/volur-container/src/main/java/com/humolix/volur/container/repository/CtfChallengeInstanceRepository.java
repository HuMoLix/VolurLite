package com.humolix.volur.container.repository;

import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CtfChallengeInstanceRepository extends JpaRepository<CtfChallengeInstance, Long>, JpaSpecificationExecutor<CtfChallengeInstance> {

    Optional<CtfChallengeInstance> findByChallengeIdAndTargetId(Long challengeId, Long targetId);

    List<CtfChallengeInstance> findByTargetId(Long targetId);

    List<CtfChallengeInstance> findByExpiresAtBeforeAndStatus(Instant now, InstanceStartupStatus status);

    long countByTargetIdAndStatus(Long targetId, InstanceStartupStatus status);

    long countByStatus(InstanceStartupStatus status);

    long countByStatusIn(Collection<InstanceStartupStatus> statuses);

    long countByCreatedAtAfter(Instant after);

    long countByNodeIpAndStatusIn(String nodeIp, Collection<InstanceStartupStatus> statuses);

    List<CtfChallengeInstance> findByNodeIp(String nodeIp);

    void deleteByChallengeIdAndTargetId(Long challengeId, Long targetId);

}
