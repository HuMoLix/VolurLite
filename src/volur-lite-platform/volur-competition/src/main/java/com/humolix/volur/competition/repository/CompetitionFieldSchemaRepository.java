package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionFieldSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionFieldSchemaRepository extends JpaRepository<CompetitionFieldSchema, Long> {

    List<CompetitionFieldSchema> findByCompetitionIdOrderBySortOrderAsc(Long competitionId);

    Optional<CompetitionFieldSchema> findByCompetitionIdAndFieldKey(Long competitionId, String fieldKey);

    boolean existsByCompetitionIdAndFieldKey(Long competitionId, String fieldKey);

    void deleteByCompetitionId(Long competitionId);

}
