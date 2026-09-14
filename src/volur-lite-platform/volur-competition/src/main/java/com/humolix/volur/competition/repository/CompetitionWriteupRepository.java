package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionWriteup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionWriteupRepository extends JpaRepository<CompetitionWriteup, Long>, JpaSpecificationExecutor<CompetitionWriteup> {

    List<CompetitionWriteup> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId);

    Optional<CompetitionWriteup> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    void deleteByCompetitionId(Long competitionId);

}
