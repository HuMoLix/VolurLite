package com.humolix.volur.competition.repository.ctf;

import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CtfCompetitionSubmissionRepository extends JpaRepository<CtfCompetitionSubmission, Long> {

    List<CtfCompetitionSubmission> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId);

    Page<CtfCompetitionSubmission> findByCompetitionIdOrderByCreatedAtDesc(Long competitionId, Pageable pageable);

    List<CtfCompetitionSubmission> findByCompetitionIdAndTeamIdOrderByCreatedAtDesc(Long competitionId, Long teamId);

}
