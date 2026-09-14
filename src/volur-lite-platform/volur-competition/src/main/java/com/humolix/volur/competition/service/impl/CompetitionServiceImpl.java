package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.CompetitionService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMapper competitionMapper;

    @Override
    public PageResponse<CompetitionSummaryVO> listCompetitions(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.DRAFT));

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("slug"), kw)
                ));
            }
            query.orderBy(cb.desc(root.get("startTime")), cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CompetitionSummaryVO> pageResult = competitionRepository.findAll(spec, pageable)
                .map(comp -> {
                    if (comp.refreshStatus()) {
                        competitionRepository.save(comp);
                    }
                    return competitionMapper.toSummaryVO(comp);
                });

        return PageResponse.of(pageResult);
    }

    @Override
    public CompetitionDetailVO getCompetitionBySlug(String slug) {
        Competition competition = competitionRepository.findBySlugWithDetails(slug).orElse(null);
        if (competition == null) {
            return null;
        }
        if (competition.refreshStatus()) {
            competitionRepository.save(competition);
        }
        if (competition.getStatus() == CompetitionStatus.DRAFT) {
            return null;
        }
        return buildDetailVO(competition);
    }

    @Override
    public CompetitionDetailVO getCompetitionById(Long id) {
        Competition competition = competitionRepository.findByIdWithDetails(id).orElse(null);
        if (competition == null) {
            return null;
        }
        if (competition.refreshStatus()) {
            competitionRepository.save(competition);
        }
        if (competition.getStatus() == CompetitionStatus.DRAFT) {
            return null;
        }
        return buildDetailVO(competition);
    }

    @Override
    public CompetitionAboutVO getCompetitionAbout(Long id) {
        Competition competition = competitionRepository.findById(id).orElse(null);
        if (competition == null) {
            return null;
        }
        if (competition.refreshStatus()) {
            competitionRepository.save(competition);
        }
        if (competition.getStatus() == CompetitionStatus.DRAFT) {
            return null;
        }
        return competitionMapper.toAboutVO(competition);
    }

    private CompetitionDetailVO buildDetailVO(Competition competition) {
        CompetitionDetailVO vo = competitionMapper.toDetailVO(competition);
        long teamCount = teamRepository.countByCompetitionIdAndStatus(competition.getId(), TeamStatus.AUDIT_PASSED);
        vo.setTeamCount(teamCount);
        return vo;
    }

}
