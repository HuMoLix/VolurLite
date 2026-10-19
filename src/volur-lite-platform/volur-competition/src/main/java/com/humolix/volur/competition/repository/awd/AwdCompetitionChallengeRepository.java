package com.humolix.volur.competition.repository.awd;

import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwdCompetitionChallengeRepository extends JpaRepository<AwdCompetitionChallenge, Long> {

    List<AwdCompetitionChallenge> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    @Query("SELECT c FROM AwdCompetitionChallenge c " +
            "JOIN FETCH c.challenge " +
            "WHERE c.competition.id = :competitionId " +
            "ORDER BY c.sortOrder ASC")
    List<AwdCompetitionChallenge> findByCompetitionIdWithChallenge(@Param("competitionId") Long competitionId);

    Optional<AwdCompetitionChallenge> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

}
