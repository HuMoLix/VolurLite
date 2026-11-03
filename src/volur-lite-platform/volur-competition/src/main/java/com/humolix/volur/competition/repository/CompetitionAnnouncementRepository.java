package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionAnnouncementRepository extends JpaRepository<CompetitionAnnouncement, Long>, JpaSpecificationExecutor<CompetitionAnnouncement> {

    List<CompetitionAnnouncement> findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(Long competitionId);

    void deleteByCompetitionId(Long competitionId);

}
