package com.humolix.volur.competition.repository.awd;

import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwdCompetitionLogRepository extends JpaRepository<AwdCompetitionLog, Long> {

    List<AwdCompetitionLog> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId);

    List<AwdCompetitionLog> findByCompetitionIdAndRoundNumberOrderByCreatedAtDesc(Long competitionId, Integer roundNumber);

    List<AwdCompetitionLog> findByCompetitionIdAndAttackerTeamId(Long competitionId, Long attackerTeamId);

    List<AwdCompetitionLog> findByCompetitionIdAndVictimTeamId(Long competitionId, Long victimTeamId);

}
