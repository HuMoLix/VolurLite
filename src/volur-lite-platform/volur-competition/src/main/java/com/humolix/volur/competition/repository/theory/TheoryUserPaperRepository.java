package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryUserPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TheoryUserPaperRepository extends JpaRepository<TheoryUserPaper, Long> {

    Optional<TheoryUserPaper> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    List<TheoryUserPaper> findByCompetitionIdAndTeam_IdIn(Long competitionId, Collection<Long> teamIds);

    Optional<TheoryUserPaper> findFirstByTeam_Id(Long teamId);

    boolean existsByCompetitionIdAndUserId(Long competitionId, Long userId);

    List<TheoryUserPaper> findByCompetitionId(Long competitionId);

    void deleteByCompetitionId(Long competitionId);

}
