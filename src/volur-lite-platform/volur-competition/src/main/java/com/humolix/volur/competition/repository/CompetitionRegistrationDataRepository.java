package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionRegistrationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionRegistrationDataRepository extends JpaRepository<CompetitionRegistrationData, Long> {

    List<CompetitionRegistrationData> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    List<CompetitionRegistrationData> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    void deleteByTeamId(Long teamId);

    void deleteByCompetitionIdAndUserId(Long competitionId, Long userId);

}
