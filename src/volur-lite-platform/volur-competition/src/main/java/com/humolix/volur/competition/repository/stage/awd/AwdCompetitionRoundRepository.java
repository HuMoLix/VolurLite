package com.humolix.volur.competition.repository.stage.awd;

import com.humolix.volur.competition.domain.entity.stage.awd.AwdCompetitionRound;
import com.humolix.volur.competition.domain.enums.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwdCompetitionRoundRepository extends JpaRepository<AwdCompetitionRound, Long> {

    List<AwdCompetitionRound> findByCompetitionIdOrderByRoundNumberAsc(Long competitionId);

    Optional<AwdCompetitionRound> findByCompetitionIdAndRoundNumber(Long competitionId, Integer roundNumber);

    Optional<AwdCompetitionRound> findFirstByCompetitionIdAndStatus(Long competitionId, StageStatus status);

}
