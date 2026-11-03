package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionOperationLogRepository extends JpaRepository<CompetitionOperationLog, Long>, JpaSpecificationExecutor<CompetitionOperationLog> {

    Optional<CompetitionOperationLog> findByIdAndCompetitionId(Long id, Long competitionId);

    long countByCompetitionId(Long competitionId);

    long countByCompetitionIdAndLogType(Long competitionId, String logType);

    long countByCompetitionIdAndStatus(Long competitionId, Boolean status);

}
