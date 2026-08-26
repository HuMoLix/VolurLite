package com.humolix.volur.competition.repository.stage.awd;

import com.humolix.volur.competition.domain.entity.stage.awd.AwdCompetitionLog;
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
