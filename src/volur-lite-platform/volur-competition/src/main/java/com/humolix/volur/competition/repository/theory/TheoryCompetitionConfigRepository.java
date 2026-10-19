package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheoryCompetitionConfigRepository extends JpaRepository<TheoryCompetitionConfig, Long> {

    Optional<TheoryCompetitionConfig> findByCompetitionId(Long competitionId);

    void deleteByCompetitionId(Long competitionId);

}
