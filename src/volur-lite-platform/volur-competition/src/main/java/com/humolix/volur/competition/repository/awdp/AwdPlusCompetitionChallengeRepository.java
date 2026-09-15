package com.humolix.volur.competition.repository.awdp;

import com.humolix.volur.competition.domain.entity.awdp.AwdPlusCompetitionChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwdPlusCompetitionChallengeRepository extends JpaRepository<AwdPlusCompetitionChallenge, Long> {

    List<AwdPlusCompetitionChallenge> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    @Query("SELECT c FROM AwdPlusCompetitionChallenge c " +
            "JOIN FETCH c.challenge " +
            "WHERE c.competition.id = :competitionId " +
            "ORDER BY c.sortOrder ASC")
    List<AwdPlusCompetitionChallenge> findByCompetitionIdWithChallenge(@Param("competitionId") Long competitionId);

    Optional<AwdPlusCompetitionChallenge> findByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

    boolean existsByCompetitionIdAndChallengeId(Long competitionId, Long challengeId);

}
