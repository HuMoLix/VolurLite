package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionMemberRepository extends JpaRepository<CompetitionMember, Long> {

    Optional<CompetitionMember> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    Optional<CompetitionMember> findByCompetitionIdAndTeamIdAndUserId(Long competitionId, Long teamId, Long userId);

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    boolean existsByCompetitionIdAndUserIdAndStatus(Long competitionId, Long userId, MemberStatus status);

    List<CompetitionMember> findByTeamId(Long teamId);

    List<CompetitionMember> findByTeamIdAndStatus(Long teamId, MemberStatus status);

    long countByTeamId(Long teamId);

    long countByTeamIdAndStatus(Long teamId, MemberStatus status);

    @Query("SELECT m FROM CompetitionMember m " +
            "JOIN FETCH m.team t " +
            "WHERE m.competition.id = :competitionId AND m.user.id = :userId")
    Optional<CompetitionMember> findMemberWithTeam(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    void deleteByTeamId(Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

}
