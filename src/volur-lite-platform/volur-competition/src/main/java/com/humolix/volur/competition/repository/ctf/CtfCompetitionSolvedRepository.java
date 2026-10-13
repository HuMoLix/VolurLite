package com.humolix.volur.competition.repository.ctf;

import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CtfCompetitionSolvedRepository extends JpaRepository<CtfCompetitionSolved, Long> {

    List<CtfCompetitionSolved> findByCompetitionId(Long competitionId);

    List<CtfCompetitionSolved> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    List<CtfCompetitionSolved> findByCompetitionChallengeIdOrderBySolvedAtAsc(Long competitionChallengeId);

    Optional<CtfCompetitionSolved> findByCompetitionIdAndCompetitionChallengeIdAndTeamId(Long competitionId, Long competitionChallengeId, Long teamId);

    boolean existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(Long competitionId, Long competitionChallengeId, Long teamId);

    long countByCompetitionChallengeId(Long competitionChallengeId);

    @Query("SELECT s FROM CtfCompetitionSolved s " +
            "JOIN FETCH s.team t " +
            "JOIN FETCH s.competitionChallenge cc " +
            "JOIN FETCH cc.challenge " +
            "WHERE s.competition.id = :competitionId " +
            "ORDER BY s.solvedAt ASC")
    List<CtfCompetitionSolved> findAllSolvesWithDetails(@Param("competitionId") Long competitionId);

    @Query("SELECT s FROM CtfCompetitionSolved s " +
            "JOIN FETCH s.team t " +
            "JOIN FETCH s.user u " +
            "JOIN FETCH s.competitionChallenge cc " +
            "JOIN FETCH cc.challenge c " +
            "LEFT JOIN FETCH c.category " +
            "WHERE s.competition.id = :competitionId AND s.team.id = :teamId " +
            "ORDER BY s.solvedAt DESC")
    List<CtfCompetitionSolved> findByCompetitionIdAndTeamIdWithDetails(
            @Param("competitionId") Long competitionId,
            @Param("teamId") Long teamId);

    @Query("SELECT s FROM CtfCompetitionSolved s " +
            "JOIN FETCH s.team t " +
            "WHERE s.competitionChallenge.id = :challengeId " +
            "ORDER BY s.solvedAt ASC")
    List<CtfCompetitionSolved> findByCompetitionChallengeIdWithDetails(@Param("challengeId") Long challengeId);

}
