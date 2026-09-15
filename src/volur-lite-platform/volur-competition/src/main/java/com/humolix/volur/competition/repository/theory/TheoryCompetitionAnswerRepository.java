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

    Optional<TheoryCompetitionAnswer> findByPaperIdAndUserId(Long paperId, Long userId);

    boolean existsByPaperIdAndUserId(Long paperId, Long userId);

}
