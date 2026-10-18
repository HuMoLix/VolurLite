package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionMemberRepository extends JpaRepository<CompetitionMember, Long> {

    Optional<CompetitionMember> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    List<CompetitionMember> findAllByCompetitionIdAndUserId(Long competitionId, Long userId);

    Optional<CompetitionMember> findByCompetitionIdAndTeamIdAndUserId(Long competitionId, Long teamId, Long userId);

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    boolean existsByCompetitionIdAndUserIdAndStatus(Long competitionId, Long userId, MemberStatus status);

    List<CompetitionMember> findByTeamId(Long teamId);

    List<CompetitionMember> findByTeamIdAndStatus(Long teamId, MemberStatus status);

    long countByTeamId(Long teamId);

    long countByTeamIdAndStatus(Long teamId, MemberStatus status);

    @Query("SELECT m FROM CompetitionMember m " +
            "JOIN FETCH m.team t " +
            "JOIN FETCH t.competition c " +
            "LEFT JOIN FETCH t.captain cap " +
            "WHERE m.competition.id = :competitionId AND m.user.id = :userId")
    Optional<CompetitionMember> findMemberWithTeam(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    @Query("SELECT m FROM CompetitionMember m " +
            "JOIN FETCH m.team t " +
            "JOIN FETCH t.competition c " +
            "LEFT JOIN FETCH t.captain cap " +
            "WHERE m.competition.id = :competitionId AND m.user.id = :userId")
    List<CompetitionMember> findMembersWithTeam(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    @Query("SELECT m FROM CompetitionMember m " +
            "JOIN FETCH m.team t " +
            "JOIN FETCH t.captain c " +
            "WHERE m.competition.id = :competitionId AND m.user.id = :userId AND m.status = :status")
    List<CompetitionMember> findUserInvitations(
            @Param("competitionId") Long competitionId,
            @Param("userId") Long userId,
            @Param("status") MemberStatus status);

    @Query("SELECT m FROM CompetitionMember m " +
            "WHERE m.competition.id = :competitionId " +
            "  AND m.status IN :activeStatuses " +
            "  AND (:excludeTeamId IS NULL OR m.team.id != :excludeTeamId) " +
            "  AND (m.phone IN :phones OR UPPER(m.idCard) IN :idCards OR LOWER(m.email) IN :emails)")
    List<CompetitionMember> findConflictingMembers(
            @Param("competitionId") Long competitionId,
            @Param("activeStatuses") Collection<MemberStatus> activeStatuses,
            @Param("excludeTeamId") Long excludeTeamId,
            @Param("phones") Collection<String> phones,
            @Param("idCards") Collection<String> idCards,
            @Param("emails") Collection<String> emails);

    void deleteByTeamId(Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

    @Query("SELECT m.competition.id, COUNT(m.id) FROM CompetitionMember m " +
            "WHERE m.competition.id IN :competitionIds AND m.status != :excludeStatus " +
            "GROUP BY m.competition.id")
    List<Object[]> countParticipantsByCompetitionIds(
            @Param("competitionIds") Collection<Long> competitionIds,
            @Param("excludeStatus") MemberStatus excludeStatus);

    @Query("SELECT m.team.id, COUNT(m.id) FROM CompetitionMember m " +
            "WHERE m.team.id IN :teamIds AND m.status = :status " +
            "GROUP BY m.team.id")
    List<Object[]> countMembersByTeamIdsAndStatus(
            @Param("teamIds") Collection<Long> teamIds,
            @Param("status") MemberStatus status);

    @Query("SELECT m FROM CompetitionMember m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE m.team.id = :teamId")
    List<CompetitionMember> findByTeamIdWithUserAndProfile(@Param("teamId") Long teamId);

    @Query("SELECT m FROM CompetitionMember m " +
            "LEFT JOIN FETCH m.user u " +
            "LEFT JOIN FETCH m.team t " +
            "WHERE m.competition.id = :competitionId")
    List<CompetitionMember> findByCompetitionIdWithUserAndTeam(@Param("competitionId") Long competitionId);

}
