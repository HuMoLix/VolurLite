package com.humolix.volur.competition.repository.isw;

import com.humolix.volur.competition.domain.entity.isw.IswCompetitionChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IswCompetitionChallengeRepository extends JpaRepository<IswCompetitionChallenge, Long> {

    List<IswCompetitionChallenge> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    @Query("SELECT c FROM IswCompetitionChallenge c " +
            "JOIN FETCH c.challenge " +
            "WHERE c.competition.id = :competitionId " +
            "ORDER BY c.sortOrder ASC")
    List<IswCompetitionChallenge> findByCompetitionIdWithChallenge(@Param("competitionId") Long competitionId);

    Optional<IswCompetitionChallenge> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

}
