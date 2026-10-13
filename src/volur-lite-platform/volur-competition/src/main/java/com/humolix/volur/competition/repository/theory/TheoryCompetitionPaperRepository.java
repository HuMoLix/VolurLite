package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheoryCompetitionPaperRepository extends JpaRepository<TheoryCompetitionPaper, Long> {

    List<TheoryCompetitionPaper> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    @Query("SELECT p FROM TheoryCompetitionPaper p " +
            "JOIN FETCH p.challenge " +
            "WHERE p.competition.id = :competitionId " +
            "ORDER BY p.sortOrder ASC")
    List<TheoryCompetitionPaper> findByCompetitionIdWithChallenge(@Param("competitionId") Long competitionId);

    @Query("SELECT p FROM TheoryCompetitionPaper p " +
            "JOIN FETCH p.challenge " +
            "WHERE p.userPaper.id = :userPaperId " +
            "ORDER BY p.sortOrder ASC")
    List<TheoryCompetitionPaper> findByUserPaperIdWithChallenge(@Param("userPaperId") Long userPaperId);

    List<TheoryCompetitionPaper> findByUserPaperIdOrderBySortOrderAsc(Long userPaperId);

    List<TheoryCompetitionPaper> findByCompetitionIdAndUserIdOrderBySortOrderAsc(Long competitionId, Long userId);

    Optional<TheoryCompetitionPaper> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    void deleteByCompetitionId(Long competitionId);

    void deleteByUserPaperId(Long userPaperId);

}
