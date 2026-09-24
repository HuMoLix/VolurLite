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
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
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

import com.humolix.volur.competition.domain.entity.CompetitionAnnouncement;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.repository.CompetitionAnnouncementRepository;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionAnnouncementRepository announcementRepository;
    private final CompetitionMapper competitionMapper;
    private final VolurCacheTemplate cacheTemplate;

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
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "detail:slug:" + slug,
                () -> {
                    Competition competition = competitionRepository.findBySlugWithDetails(slug).orElse(null);
                    if (competition == null) {
                        return null;
                    }
                    try {
                        if (competition.refreshStatus()) {
                            competitionRepository.save(competition);
                        }
                    } catch (Exception ignored) {
                        // 抑制高并发读时的乐观锁或并发写入异常
                    }
                    if (competition.getStatus() == CompetitionStatus.DRAFT) {
                        return null;
                    }
                    return buildDetailVO(competition);
                }
        );
    }

    @Override
    public CompetitionDetailVO getCompetitionById(Long id) {
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "detail:id:" + id,
                () -> {
                    Competition competition = competitionRepository.findByIdWithDetails(id).orElse(null);
                    if (competition == null) {
                        return null;
                    }
                    try {
                        if (competition.refreshStatus()) {
                            competitionRepository.save(competition);
                        }
                    } catch (Exception ignored) {
                        // 抑制高并发读时的乐观锁或并发写入异常
                    }
                    if (competition.getStatus() == CompetitionStatus.DRAFT) {
                        return null;
                    }
                    return buildDetailVO(competition);
                }
        );
    }

    @Override
    public CompetitionAboutVO getCompetitionAbout(Long id) {
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "about:" + id,
                () -> {
                    Competition competition = competitionRepository.findById(id).orElse(null);
                    if (competition == null) {
                        return null;
                    }
                    try {
                        if (competition.refreshStatus()) {
                            competitionRepository.save(competition);
                        }
                    } catch (Exception ignored) {
                    }
                    if (competition.getStatus() == CompetitionStatus.DRAFT) {
                        return null;
                    }
                    return competitionMapper.toAboutVO(competition);
                }
        );
    }

    @Override
    public List<CompetitionAnnouncementVO> listAnnouncements(Long competitionId) {
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "announcements:" + competitionId,
                () -> {
                    if (!competitionRepository.existsById(competitionId)) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在");
                    }
                    List<CompetitionAnnouncement> list = announcementRepository.findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(competitionId);
                    return competitionMapper.toAnnouncementVOList(list);
                }
        );
    }

    private CompetitionDetailVO buildDetailVO(Competition competition) {
        CompetitionDetailVO vo = competitionMapper.toDetailVO(competition);
        long teamCount = teamRepository.countByCompetitionIdAndStatus(competition.getId(), TeamStatus.AUDIT_PASSED);
        vo.setTeamCount(teamCount);
        return vo;
    }

}
