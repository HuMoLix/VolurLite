package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionTeamInvitation;
import com.humolix.volur.competition.domain.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionTeamInvitationRepository extends JpaRepository<CompetitionTeamInvitation, Long>, JpaSpecificationExecutor<CompetitionTeamInvitation> {

    List<CompetitionTeamInvitation> findByCompetitionIdAndInviteeIdOrderByCreatedAtDesc(Long competitionId, Long inviteeId);

    List<CompetitionTeamInvitation> findByCompetitionIdAndTeamIdOrderByCreatedAtDesc(Long competitionId, Long teamId);

    Optional<CompetitionTeamInvitation> findByIdAndCompetitionId(Long id, Long competitionId);

    boolean existsByCompetitionIdAndTeamIdAndInviteeIdAndStatus(Long competitionId, Long teamId, Long inviteeId, InvitationStatus status);

}
