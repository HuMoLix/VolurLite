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
            "JOIN FETCH p.challenge c " +
            "LEFT JOIN FETCH c.bank " +
            "WHERE p.competition.id = :competitionId " +
            "ORDER BY p.sortOrder ASC")
    List<TheoryCompetitionPaper> findByCompetitionIdWithChallenge(@Param("competitionId") Long competitionId);

    @Query("SELECT p FROM TheoryCompetitionPaper p " +
            "JOIN FETCH p.challenge c " +
            "LEFT JOIN FETCH c.bank " +
            "WHERE p.userPaper.id = :userPaperId " +
            "ORDER BY p.sortOrder ASC")
    List<TheoryCompetitionPaper> findByUserPaperIdWithChallenge(@Param("userPaperId") Long userPaperId);

    List<TheoryCompetitionPaper> findByUserPaperIdOrderBySortOrderAsc(Long userPaperId);

    @Query("SELECT p FROM TheoryCompetitionPaper p " +
            "JOIN FETCH p.challenge c " +
            "LEFT JOIN FETCH c.bank " +
            "WHERE p.competition.id = :competitionId AND p.user.id = :userId " +
            "ORDER BY p.sortOrder ASC")
    List<TheoryCompetitionPaper> findByCompetitionIdAndUserIdOrderBySortOrderAsc(@Param("competitionId") Long competitionId, @Param("userId") Long userId);

    Optional<TheoryCompetitionPaper> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    void deleteByCompetitionId(Long competitionId);

    void deleteByUserPaperId(Long userPaperId);

}
