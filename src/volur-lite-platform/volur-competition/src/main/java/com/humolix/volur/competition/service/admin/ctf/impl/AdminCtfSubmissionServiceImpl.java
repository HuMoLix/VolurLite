package com.humolix.volur.competition.service.admin.ctf.impl;

import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSubmission;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionSubmissionVO;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository;
import com.humolix.volur.competition.service.admin.ctf.AdminCtfSubmissionService;
import com.humolix.volur.framework.common.api.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCtfSubmissionServiceImpl implements AdminCtfSubmissionService {

    private final CtfCompetitionSubmissionRepository submissionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CtfCompetitionSubmissionVO> listSubmissions(
            Long competitionId,
            int page,
            int size,
            Long challengeId,
            String keyword) {

        int validPage = Math.max(page, 1);
        int validSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(validPage - 1, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<CtfCompetitionSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("competition").get("id"), competitionId));

            if (challengeId != null) {
                Predicate byCompChalId = cb.equal(root.get("competitionChallenge").get("id"), challengeId);
                Predicate byBankChalId = cb.equal(root.get("competitionChallenge").get("challenge").get("id"), challengeId);
                predicates.add(cb.or(byCompChalId, byBankChalId));
            }

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                Predicate teamMatch = cb.like(cb.lower(root.get("team").get("teamName")), kw);
                Predicate userMatch = cb.like(cb.lower(root.get("user").get("username")), kw);
                Predicate flagMatch = cb.like(cb.lower(root.get("submittedFlag")), kw);
                Predicate challengeMatch = cb.like(cb.lower(root.get("competitionChallenge").get("challenge").get("name")), kw);
                predicates.add(cb.or(teamMatch, userMatch, flagMatch, challengeMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CtfCompetitionSubmission> pageResult = submissionRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(this::toVO));
    }

    private CtfCompetitionSubmissionVO toVO(CtfCompetitionSubmission sub) {
        String challengeName = null;
        String categoryName = null;
        if (sub.getCompetitionChallenge() != null && sub.getCompetitionChallenge().getChallenge() != null) {
            challengeName = sub.getCompetitionChallenge().getChallenge().getName();
            if (sub.getCompetitionChallenge().getChallenge().getCategory() != null) {
                categoryName = sub.getCompetitionChallenge().getChallenge().getCategory().getName();
            }
        }

        return CtfCompetitionSubmissionVO.builder()
                .id(sub.getId())
                .userId(sub.getUser() != null ? sub.getUser().getId() : null)
                .username(sub.getUser() != null ? sub.getUser().getUsername() : null)
                .teamId(sub.getTeam() != null ? sub.getTeam().getId() : null)
                .teamName(sub.getTeam() != null ? sub.getTeam().getTeamName() : null)
                .challengeId(sub.getCompetitionChallenge() != null ? sub.getCompetitionChallenge().getId() : null)
                .challengeName(challengeName)
                .challengeCategory(categoryName)
                .submittedFlag(sub.getSubmittedFlag())
                .status(sub.getStatus())
                .createdAt(sub.getCreatedAt())
                .build();
    }

}
