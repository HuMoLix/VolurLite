package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionConfigRepository extends JpaRepository<CompetitionConfig, Long> {

    Optional<CompetitionConfig> findByCompetitionId(Long competitionId);

}
