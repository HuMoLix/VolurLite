package com.humolix.volur.container.repository;

import com.humolix.volur.container.entity.CtfChallengeInstance;
import com.humolix.volur.container.entity.enums.InstanceStartupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CtfChallengeInstanceRepository extends JpaRepository<CtfChallengeInstance, Long> {

    Optional<CtfChallengeInstance> findByChallengeIdAndTargetId(Long challengeId, Long targetId);

    List<CtfChallengeInstance> findByTargetId(Long targetId);

    List<CtfChallengeInstance> findByExpiresAtBeforeAndStatus(Instant now, InstanceStartupStatus status);

    long countByTargetIdAndStatus(Long targetId, InstanceStartupStatus status);

    void deleteByChallengeIdAndTargetId(Long challengeId, Long targetId);

}
