package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long>, JpaSpecificationExecutor<CompetitionTeam> {

    Optional<CompetitionTeam> findByCompetitionIdAndInviteCode(Long competitionId, String inviteCode);

    Optional<CompetitionTeam> findByCompetitionIdAndTeamName(Long competitionId, String teamName);

    boolean existsByCompetitionIdAndTeamName(Long competitionId, String teamName);

    boolean existsByCompetitionIdAndCaptain_Id(Long competitionId, Long captainId);

    Optional<CompetitionTeam> findByCompetitionIdAndCaptain_Id(Long competitionId, Long captainId);

    long countByCompetitionId(Long competitionId);

    List<CompetitionTeam> findByCompetitionIdAndStatus(Long competitionId, TeamStatus status);

    long countByCompetitionIdAndStatus(Long competitionId, TeamStatus status);

    @Query("SELECT t FROM CompetitionTeam t " +
            "JOIN FETCH t.members m " +
            "JOIN FETCH m.user " +
            "WHERE t.id = :id")
    Optional<CompetitionTeam> findByIdWithMembers(@Param("id") Long id);

}
