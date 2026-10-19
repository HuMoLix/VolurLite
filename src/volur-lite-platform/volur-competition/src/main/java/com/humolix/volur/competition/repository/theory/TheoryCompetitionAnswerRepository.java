package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheoryCompetitionAnswerRepository extends JpaRepository<TheoryCompetitionAnswer, Long> {

    List<TheoryCompetitionAnswer> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    List<TheoryCompetitionAnswer> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    List<TheoryCompetitionAnswer> findByCompetitionId(Long competitionId);

    Optional<TheoryCompetitionAnswer> findByPaperIdAndUserId(Long paperId, Long userId);

    boolean existsByPaperIdAndUserId(Long paperId, Long userId);

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    boolean existsByTeamId(Long teamId);

    boolean existsByCompetitionId(Long competitionId);

    long countByCompetitionId(Long competitionId);

}
