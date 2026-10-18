package com.humolix.volur.competition.service.admin.impl;

import com.humolix.volur.competition.domain.dto.CompetitionConfigDTO;
import com.humolix.volur.competition.domain.dto.CompetitionCreateRequestDTO;
import com.humolix.volur.competition.domain.dto.CompetitionUpdateRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.vo.AdminCompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionListVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.admin.AdminCompetitionService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCompetitionServiceImpl implements AdminCompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;
    private final VolurCacheTemplate cacheTemplate;

    @Override
    public PageResponse<AdminCompetitionListVO> listCompetitions(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("slug"), kw)
                ));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Competition> compPage = competitionRepository.findAll(spec, pageable);

        // 1. 批量检测并保存状态变更，消除遍历中的多次单条写库
        List<Competition> updatedComps = compPage.getContent().stream()
                .filter(Competition::refreshStatus)
                .toList();
        if (!updatedComps.isEmpty()) {
            competitionRepository.saveAll(updatedComps);
        }

        // 2. 批量聚合查询队伍数与有效参赛人数，消除循环内的 N+1 查询并修复 0 队/0 人问题
        List<Long> compIds = compPage.getContent().stream().map(Competition::getId).toList();
        Map<Long, Long> teamCountMap = new HashMap<>();
        Map<Long, Long> participantCountMap = new HashMap<>();

        if (!compIds.isEmpty()) {
            List<Object[]> teamCounts = teamRepository.countTeamsByCompetitionIds(compIds);
            for (Object[] row : teamCounts) {
                teamCountMap.put((Long) row[0], ((Number) row[1]).longValue());
            }

            List<Object[]> participantCounts = memberRepository.countParticipantsByCompetitionIds(compIds, MemberStatus.NOT_PARTICIPATE);
            for (Object[] row : participantCounts) {
                participantCountMap.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        return PageResponse.of(compPage.map(comp -> {
            AdminCompetitionListVO vo = competitionMapper.toAdminListVO(comp);
            vo.setTeamCount(teamCountMap.getOrDefault(comp.getId(), 0L));
            vo.setParticipantCount(participantCountMap.getOrDefault(comp.getId(), 0L));
            return vo;
        }));
    }

    @Override
    @Transactional
    public AdminCompetitionDetailVO getCompetition(Long id) {
        Competition competition = competitionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));
        if (competition.refreshStatus()) {
            competitionRepository.save(competition);
        }
        return competitionMapper.toAdminDetailVO(competition);
    }

    @Override
    @Transactional
    public Long createCompetition(CompetitionCreateRequestDTO request, Long userId) {
        if (competitionRepository.existsBySlug(request.getSlug().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "竞赛 Slug 已存在");
        }

        // 校验比赛起止时间与报名起止时间约束
        validateCompetitionSchedule(request.getStartTime(), request.getEndTime(), request.getRegStartTime(), request.getRegEndTime(), true);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // 管理员创建竞赛时不可选择竞赛状态，强制初始化为 DRAFT
        Competition competition = Competition.builder()
                .title(request.getTitle().trim())
                .slug(request.getSlug().trim())
                .type(request.getType() != null ? request.getType() : CompetitionType.CTF)
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .status(CompetitionStatus.DRAFT)
                .regStartTime(request.getRegStartTime())
                .regEndTime(request.getRegEndTime())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .createdBy(user)
                .build();

        Competition savedComp = competitionRepository.save(competition);

        // Save Config
        CompetitionConfigDTO cDTO = request.getConfig();
        CompetitionConfig config = CompetitionConfig.builder()
                .competition(savedComp)
                .maxLeaders(cDTO != null && cDTO.getMaxLeaders() != null ? cDTO.getMaxLeaders() : 1)
                .maxTeamMembers(cDTO != null && cDTO.getMaxTeamMembers() != null ? cDTO.getMaxTeamMembers() : 4)
                .minTeamMembers(cDTO != null && cDTO.getMinTeamMembers() != null ? cDTO.getMinTeamMembers() : 1)
                .maxTeams(cDTO != null && cDTO.getMaxTeams() != null ? cDTO.getMaxTeams() : 500)
                .requireTeamAudit(cDTO != null && cDTO.getRequireTeamAudit() != null && cDTO.getRequireTeamAudit())
                .freezeScoreboardMinutes(cDTO != null && cDTO.getFreezeScoreboardMinutes() != null ? cDTO.getFreezeScoreboardMinutes() : 0)
                .isScoreboardFrozen(false)
                .allowWriteupSubmission(cDTO == null || cDTO.getAllowWriteupSubmission() == null || cDTO.getAllowWriteupSubmission())
                .maxContainersPerTeam(cDTO != null && cDTO.getMaxContainersPerTeam() != null ? cDTO.getMaxContainersPerTeam() : 2)
                .writeupTemplateUrl(cDTO != null ? cDTO.getWriteupTemplateUrl() : null)
                .writeupTemplateName(cDTO != null ? cDTO.getWriteupTemplateName() : null)
                .writeupStartTime(cDTO != null ? cDTO.getWriteupStartTime() : null)
                .writeupEndTime(cDTO != null ? cDTO.getWriteupEndTime() : null)
                .regRequirements(cDTO != null ? cDTO.getRegRequirements() : null)
                .enableBloodBonus(cDTO == null || cDTO.getEnableBloodBonus() == null || cDTO.getEnableBloodBonus())
                .firstBloodBonusPercentage(cDTO != null && cDTO.getFirstBloodBonusPercentage() != null ? cDTO.getFirstBloodBonusPercentage() : 10)
                .secondBloodBonusPercentage(cDTO != null && cDTO.getSecondBloodBonusPercentage() != null ? cDTO.getSecondBloodBonusPercentage() : 5)
                .thirdBloodBonusPercentage(cDTO != null && cDTO.getThirdBloodBonusPercentage() != null ? cDTO.getThirdBloodBonusPercentage() : 2)
                .build();
        configRepository.save(config);

        return savedComp.getId();
    }

    @Override
    @Transactional
    public void updateCompetition(Long id, CompetitionUpdateRequestDTO request) {
        Competition comp = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (StringUtils.hasText(request.getSlug()) && !comp.getSlug().equals(request.getSlug().trim())) {
            if (competitionRepository.existsBySlug(request.getSlug().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "竞赛 Slug 已存在");
            }
            comp.setSlug(request.getSlug().trim());
        }

        Instant newStart = request.getStartTime() != null ? request.getStartTime() : comp.getStartTime();
        Instant newEnd = request.getEndTime() != null ? request.getEndTime() : comp.getEndTime();
        Instant newRegStart = request.getRegStartTime() != null ? request.getRegStartTime() : comp.getRegStartTime();
        Instant newRegEnd = request.getRegEndTime() != null ? request.getRegEndTime() : comp.getRegEndTime();
        boolean isNotStarted = (comp.getStatus() == CompetitionStatus.DRAFT 
                || comp.getStatus() == CompetitionStatus.PUBLISHED 
                || comp.getStatus() == CompetitionStatus.REGISTRATION);
        validateCompetitionSchedule(newStart, newEnd, newRegStart, newRegEnd, isNotStarted);

        if (request.getTitle() != null) comp.setTitle(request.getTitle().trim());
        if (request.getType() != null) comp.setType(request.getType());
        if (request.getDescription() != null) comp.setDescription(request.getDescription());
        if (request.getBannerUrl() != null) comp.setBannerUrl(request.getBannerUrl());
        if (request.getRegStartTime() != null) comp.setRegStartTime(request.getRegStartTime());
        if (request.getRegEndTime() != null) comp.setRegEndTime(request.getRegEndTime());
        if (request.getStartTime() != null) comp.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) comp.setEndTime(request.getEndTime());

        if (request.getStatus() != null) {
            if (comp.getStatus() == CompetitionStatus.ARCHIVED) {
                if (request.getStatus() != CompetitionStatus.ARCHIVED) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "已归档竞赛不可直接修改状态，请先取消归档");
                }
            } else {
                if (request.getStatus() == CompetitionStatus.ARCHIVED) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "请使用专用归档接口对竞赛进行归档");
                } else if (request.getStatus() == CompetitionStatus.DRAFT) {
                    comp.setStatus(CompetitionStatus.DRAFT);
                } else if (request.getStatus() == CompetitionStatus.HIDDEN) {
                    comp.setStatus(CompetitionStatus.HIDDEN);
                } else {
                    comp.setStatus(CompetitionStatus.PUBLISHED);
                    comp.refreshStatus();
                }
            }
        } else {
            comp.refreshStatus();
        }

        competitionRepository.save(comp);

        // Update Config
        if (request.getConfig() != null) {
            CompetitionConfigDTO cDTO = request.getConfig();
            CompetitionConfig config = configRepository.findByCompetitionId(id)
                    .orElse(CompetitionConfig.builder().competition(comp).build());

            if (cDTO.getMaxLeaders() != null) config.setMaxLeaders(cDTO.getMaxLeaders());
            if (cDTO.getMaxTeamMembers() != null) config.setMaxTeamMembers(cDTO.getMaxTeamMembers());
            if (cDTO.getMinTeamMembers() != null) config.setMinTeamMembers(cDTO.getMinTeamMembers());
            if (cDTO.getMaxTeams() != null) config.setMaxTeams(cDTO.getMaxTeams());
            if (cDTO.getRequireTeamAudit() != null) config.setRequireTeamAudit(cDTO.getRequireTeamAudit());
            if (cDTO.getFreezeScoreboardMinutes() != null) config.setFreezeScoreboardMinutes(cDTO.getFreezeScoreboardMinutes());
            if (cDTO.getAllowWriteupSubmission() != null) config.setAllowWriteupSubmission(cDTO.getAllowWriteupSubmission());
            if (cDTO.getMaxContainersPerTeam() != null) {
                if (cDTO.getMaxContainersPerTeam() < 1) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "单队同时在线容器上限必须大于等于 1");
                }
                config.setMaxContainersPerTeam(cDTO.getMaxContainersPerTeam());
            }
            if (cDTO.getWriteupTemplateUrl() != null) config.setWriteupTemplateUrl(cDTO.getWriteupTemplateUrl());
            if (cDTO.getWriteupTemplateName() != null) config.setWriteupTemplateName(cDTO.getWriteupTemplateName());
            if (cDTO.getWriteupStartTime() != null) config.setWriteupStartTime(cDTO.getWriteupStartTime());
            if (cDTO.getWriteupEndTime() != null) config.setWriteupEndTime(cDTO.getWriteupEndTime());
            if (cDTO.getRegRequirements() != null) config.setRegRequirements(cDTO.getRegRequirements());
            if (cDTO.getEnableBloodBonus() != null) config.setEnableBloodBonus(cDTO.getEnableBloodBonus());
            if (cDTO.getFirstBloodBonusPercentage() != null) config.setFirstBloodBonusPercentage(cDTO.getFirstBloodBonusPercentage());
            if (cDTO.getSecondBloodBonusPercentage() != null) config.setSecondBloodBonusPercentage(cDTO.getSecondBloodBonusPercentage());
            if (cDTO.getThirdBloodBonusPercentage() != null) config.setThirdBloodBonusPercentage(cDTO.getThirdBloodBonusPercentage());

            configRepository.save(config);
        }

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + id);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "about:" + id);
    }

    @Override
    @Transactional
    public void deleteCompetition(Long id) {
        competitionRepository.findById(id).ifPresent(comp -> {
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + id);
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "about:" + id);
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:ctf:" + id);
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:composite:" + id);
            cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "announcements:" + id);
        });
        competitionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void setScoreboardFrozen(Long id, boolean frozen) {
        CompetitionConfig config = configRepository.findByCompetitionId(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛配置不存在"));
        config.setIsScoreboardFrozen(frozen);
        configRepository.save(config);

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:ctf:" + id);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:composite:" + id);
    }

    @Override
    @Transactional
    public void archiveCompetition(Long id) {
        Competition comp = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));
        comp.setStatus(CompetitionStatus.ARCHIVED);
        competitionRepository.save(comp);

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + id);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
    }

    @Override
    @Transactional
    public void unarchiveCompetition(Long id) {
        Competition comp = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));
        comp.setStatus(CompetitionStatus.DRAFT);
        competitionRepository.save(comp);

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + id);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncCompetitionStatusSchedule() {
        List<Competition> activeComps = competitionRepository.findByStatusIn(
                List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.RUNNING)
        );
        for (Competition comp : activeComps) {
            if (comp.refreshStatus()) {
                competitionRepository.save(comp);
                cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:id:" + comp.getId());
                cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "detail:slug:" + comp.getSlug());
            }
        }
    }

    private void validateCompetitionSchedule(Instant startTime, Instant endTime, Instant regStartTime, Instant regEndTime, boolean isNewOrNotStarted) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛起止时间不能为空");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛开始时间必须早于结束时间");
        }
        Instant now = Instant.now();
        // 比赛起止时间不能是过去的时间（允许 60 秒时钟误差）
        if (isNewOrNotStarted) {
            if (startTime.isBefore(now.minusSeconds(60))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛开始时间不能早于当前时间，比赛起止时间不能是过去的时间");
            }
            if (endTime.isBefore(now)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛结束时间不能早于当前时间，比赛起止时间不能是过去的时间");
            }
        }

        // 报名时间约束：比赛开始之后不可报名
        if (regStartTime != null && regEndTime != null) {
            if (!regStartTime.isBefore(regEndTime)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "报名开始时间必须早于报名截止时间");
            }
        }
        if (regEndTime != null) {
            if (regEndTime.isAfter(startTime)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "报名起止时间不能在比赛开始之后进行报名（报名截止时间必须早于或等于比赛开始时间）");
            }
        }
        if (regStartTime != null) {
            if (!regStartTime.isBefore(startTime)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "报名开始时间必须早于比赛开始时间");
            }
        }
    }

}
