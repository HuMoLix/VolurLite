package com.humolix.volur.training.repository;

import com.humolix.volur.training.domain.entity.TrainingChallenge;
import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingChallengeRepository extends JpaRepository<TrainingChallenge, Long>, JpaSpecificationExecutor<TrainingChallenge> {

    boolean existsBySourceChallengeId(Long sourceChallengeId);

    Optional<TrainingChallenge> findBySourceChallengeId(Long sourceChallengeId);

    List<TrainingChallenge> findBySourceChallengeIdIn(Collection<Long> sourceChallengeIds);

    long countByStatus(TrainingChallengeStatus status);

}
