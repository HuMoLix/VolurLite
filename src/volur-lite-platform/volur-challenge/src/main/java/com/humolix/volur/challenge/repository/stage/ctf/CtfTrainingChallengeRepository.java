package com.humolix.volur.challenge.repository.stage.ctf;

import com.humolix.volur.challenge.entity.stage.ctf.CtfTrainingChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CtfTrainingChallengeRepository extends JpaRepository<CtfTrainingChallenge, Long>, JpaSpecificationExecutor<CtfTrainingChallenge> {

    Optional<CtfTrainingChallenge> findByChallengeId(Long challengeId);

    long countByChallengeId(Long challengeId);

    @Query("SELECT tc FROM CtfTrainingChallenge tc LEFT JOIN FETCH tc.challenge WHERE tc.id = :id")
    Optional<CtfTrainingChallenge> findWithChallengeById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"challenge", "challenge.category", "challenge.author"})
    Page<CtfTrainingChallenge> findAll(Specification<CtfTrainingChallenge> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"challenge"})
    List<CtfTrainingChallenge> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE CtfTrainingChallenge c SET c.solveCount = c.solveCount + 1 WHERE c.id = :id")
    void incrementSolveCount(@Param("id") Long id);

    boolean existsByChallengeId(Long challengeId);

}
