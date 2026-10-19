package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionAnnouncement;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionAnnouncementRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.CompetitionService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.api.SliceResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CompetitionAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;
    private final VolurCacheTemplate cacheTemplate;

    @Override
    public PageResponse<CompetitionSummaryVO> listCompetitions(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.DRAFT));
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.HIDDEN));

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
                        evictCompetitionDetailCache(comp);
                    }
                    return competitionMapper.toSummaryVO(comp);
                });

        return PageResponse.of(pageResult);
    }

    @Override
    public SliceResponse<CompetitionSummaryVO> listCompetitionSlice(Integer page, Integer size, String keyword, CompetitionType type) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.DRAFT));
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.ARCHIVED));
            predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.HIDDEN));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

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
                        evictCompetitionDetailCache(comp);
                    }
                    return competitionMapper.toSummaryVO(comp);
                });

        return SliceResponse.of(pageResult);
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
                            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + competition.getId());
                            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "about:" + competition.getId());
                        }
                    } catch (Exception ignored) {
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
                            if (StringUtils.hasText(competition.getSlug())) {
                                cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + competition.getSlug());
                            }
                            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "about:" + competition.getId());
                        }
                    } catch (Exception ignored) {
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
    public List<CompetitionAnnouncementVO> listAnnouncements(Long competitionId, Long userId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在");
        }
        validateUserParticipationOrAdmin(competitionId, userId);
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "announcements:" + competitionId,
                () -> {
                    List<CompetitionAnnouncement> list = announcementRepository.findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(competitionId);
                    return competitionMapper.toAnnouncementVOList(list);
                }
        );
    }

    private CompetitionTeam getMyApprovedTeam(Long competitionId, Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionMember member = memberOpt.get();
            if (member.getStatus() == MemberStatus.INVITED_ACCEPTED) {
                CompetitionTeam team = member.getTeam();
                if (team != null && team.getStatus() == TeamStatus.BANNED) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
                }
                if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                    return team;
                }
            }
        }
        return null;
    }

    private void validateUserParticipationOrAdmin(Long competitionId, Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionMember member = memberOpt.get();
            if (member.getStatus() == MemberStatus.INVITED_ACCEPTED) {
                CompetitionTeam team = member.getTeam();
                if (team != null && team.getStatus() == TeamStatus.BANNED) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
                }
                if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                    return;
                }
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        boolean isAdmin = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        if (!isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }
    }

    private CompetitionDetailVO buildDetailVO(Competition competition) {
        CompetitionDetailVO vo = competitionMapper.toDetailVO(competition);
        long teamCount = teamRepository.countByCompetitionIdAndStatus(competition.getId(), TeamStatus.AUDIT_PASSED);
        vo.setTeamCount(teamCount);
        return vo;
    }

    private void evictCompetitionDetailCache(Competition comp) {
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + comp.getId());
        if (StringUtils.hasText(comp.getSlug())) {
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
        }
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "about:" + comp.getId());
    }

}
