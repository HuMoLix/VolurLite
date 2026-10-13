package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryUserPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheoryUserPaperRepository extends JpaRepository<TheoryUserPaper, Long> {

    Optional<TheoryUserPaper> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    void deleteByCompetitionId(Long competitionId);

}
