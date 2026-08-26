package com.humolix.volur.competition.repository.stage.ctf;

import com.humolix.volur.competition.domain.entity.stage.ctf.CtfCompetitionChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CtfCompetitionChallengeRepository extends JpaRepository<CtfCompetitionChallenge, Long>, JpaSpecificationExecutor<CtfCompetitionChallenge> {

    List<CtfCompetitionChallenge> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    List<CtfCompetitionChallenge> findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc(Long competitionId);

    List<CtfCompetitionChallenge> findByCompetitionIdAndStageIdOrderBySortOrderAsc(Long competitionId, Long stageId);

    List<CtfCompetitionChallenge> findByCompetitionIdAndStageIdAndIsVisibleTrueOrderBySortOrderAsc(Long competitionId, Long stageId);

    Optional<CtfCompetitionChallenge> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    Optional<CtfCompetitionChallenge> findByCompetitionIdAndStageIdAndChallengeId(Long competitionId, Long stageId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndStageIdAndChallengeId(Long competitionId, Long stageId, Long challengeId);

    @Query("SELECT c FROM CtfCompetitionChallenge c " +
            "JOIN FETCH c.challenge ch " +
            "LEFT JOIN FETCH ch.category " +
            "LEFT JOIN FETCH c.stage " +
            "WHERE c.id = :id")
    Optional<CtfCompetitionChallenge> findByIdWithChallenge(@Param("id") Long id);

}
