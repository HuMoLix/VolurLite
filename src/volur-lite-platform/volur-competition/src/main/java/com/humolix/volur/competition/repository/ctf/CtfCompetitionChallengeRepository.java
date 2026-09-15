package com.humolix.volur.competition.repository.ctf;

import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
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

    Optional<CtfCompetitionChallenge> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    @Query("SELECT c FROM CtfCompetitionChallenge c " +
            "JOIN FETCH c.challenge ch " +
            "LEFT JOIN FETCH ch.category " +
            "WHERE c.id = :id")
    Optional<CtfCompetitionChallenge> findByIdWithChallenge(@Param("id") Long id);

}
