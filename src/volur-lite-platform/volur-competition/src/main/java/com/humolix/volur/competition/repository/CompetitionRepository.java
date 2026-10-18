package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {

    Optional<Competition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Competition> findByStatus(CompetitionStatus status);

    List<Competition> findByStatusIn(Collection<CompetitionStatus> statuses);

    @Query("SELECT c FROM Competition c " +
            "LEFT JOIN FETCH c.config " +
            "LEFT JOIN FETCH c.createdBy " +
            "WHERE c.id = :id")
    Optional<Competition> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT c FROM Competition c " +
            "LEFT JOIN FETCH c.config " +
            "LEFT JOIN FETCH c.createdBy " +
            "WHERE c.slug = :slug")
    Optional<Competition> findBySlugWithDetails(@Param("slug") String slug);

}
