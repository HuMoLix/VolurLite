package com.humolix.volur.competition.repository.stage.awdp;

import com.humolix.volur.competition.domain.entity.stage.awdp.AwdPlusCompetitionPatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwdPlusCompetitionPatchRepository extends JpaRepository<AwdPlusCompetitionPatch, Long> {

    List<AwdPlusCompetitionPatch> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    List<AwdPlusCompetitionPatch> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

}
