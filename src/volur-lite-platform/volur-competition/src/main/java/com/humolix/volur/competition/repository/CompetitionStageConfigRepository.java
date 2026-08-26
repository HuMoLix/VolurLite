package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionStageConfig;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionStageConfigRepository extends JpaRepository<CompetitionStageConfig, Long> {

    List<CompetitionStageConfig> findByCompetitionIdOrderByStageOrderAsc(Long competitionId);

    List<CompetitionStageConfig> findByCompetitionIdAndStageOrderByStageOrderAsc(Long competitionId, CompetitionStage stage);

    Optional<CompetitionStageConfig> findFirstByCompetitionIdAndStageOrderByStageOrderAsc(Long competitionId, CompetitionStage stage);

    Optional<CompetitionStageConfig> findByCompetitionIdAndStage(Long competitionId, CompetitionStage stage);

    boolean existsByCompetitionIdAndStage(Long competitionId, CompetitionStage stage);

    void deleteByCompetitionId(Long competitionId);

}
