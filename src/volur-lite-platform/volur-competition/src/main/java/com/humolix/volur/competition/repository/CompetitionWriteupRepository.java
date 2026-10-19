package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionWriteup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionWriteupRepository extends JpaRepository<CompetitionWriteup, Long>, JpaSpecificationExecutor<CompetitionWriteup> {

    List<CompetitionWriteup> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId);

    Optional<CompetitionWriteup> findByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    List<CompetitionWriteup> findByCompetitionIdAndTeamIdOrderByCreatedAtDesc(Long competitionId, Long teamId);

    long countByCompetitionIdAndTeamId(Long competitionId, Long teamId);

    void deleteByCompetitionId(Long competitionId);

    @Query("SELECT w FROM CompetitionWriteup w " +
           "WHERE w.competitionId = :competitionId " +
           "  AND (:keyword IS NULL OR :keyword = '' OR " +
           "       LOWER(w.teamName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.createdAt DESC")
    List<CompetitionWriteup> searchByCompetitionIdAndKeyword(
            @Param("competitionId") Long competitionId,
            @Param("keyword") String keyword);

    @Query(value = "SELECT w FROM CompetitionWriteup w " +
           "WHERE w.competitionId = :competitionId " +
           "  AND (:keyword IS NULL OR :keyword = '' OR " +
           "       LOWER(w.teamName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.createdAt DESC",
           countQuery = "SELECT COUNT(w) FROM CompetitionWriteup w " +
           "WHERE w.competitionId = :competitionId " +
           "  AND (:keyword IS NULL OR :keyword = '' OR " +
           "       LOWER(w.teamName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(w.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<CompetitionWriteup> searchByCompetitionIdAndKeyword(
            @Param("competitionId") Long competitionId,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

}
