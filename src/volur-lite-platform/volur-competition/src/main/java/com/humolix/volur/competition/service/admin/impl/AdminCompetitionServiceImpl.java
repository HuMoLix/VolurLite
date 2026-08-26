package com.humolix.volur.competition.service.admin.impl;

import com.humolix.volur.competition.domain.dto.*;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionFieldSchema;
import com.humolix.volur.competition.domain.entity.CompetitionStageConfig;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.FillScope;
import com.humolix.volur.competition.domain.enums.StageStatus;
import com.humolix.volur.competition.domain.vo.AdminCompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionListVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.*;
import com.humolix.volur.competition.service.admin.AdminCompetitionService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCompetitionServiceImpl implements AdminCompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionStageConfigRepository stageConfigRepository;
    private final CompetitionFieldSchemaRepository fieldSchemaRepository;
    private final CompetitionTeamRepository teamRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;

    @Override
    public PageResponse<AdminCompetitionListVO> listCompetitions(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("title"), "%" + keyword.trim() + "%"));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(competitionRepository.findAll(spec, pageable).map(comp -> {
            AdminCompetitionListVO vo = competitionMapper.toAdminListVO(comp);
            vo.setTeamCount(teamRepository.countByCompetitionIdAndStatus(comp.getId(), null));
            return vo;
        }));
    }

    @Override
    public AdminCompetitionDetailVO getCompetition(Long id) {
        Competition competition = competitionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));
        return competitionMapper.toAdminDetailVO(competition);
    }

    @Override
    @Transactional
    public Long createCompetition(CompetitionCreateRequestDTO request, Long userId) {
        if (competitionRepository.existsBySlug(request.getSlug().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "竞赛 Slug 已存在");
        }

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Competition competition = Competition.builder()
                .title(request.getTitle().trim())
                .slug(request.getSlug().trim())
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .status(request.getStatus() != null ? request.getStatus() : CompetitionStatus.DRAFT)
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
                .maxTeamMembers(cDTO != null && cDTO.getMaxTeamMembers() != null ? cDTO.getMaxTeamMembers() : 4)
                .minTeamMembers(cDTO != null && cDTO.getMinTeamMembers() != null ? cDTO.getMinTeamMembers() : 1)
                .maxTeams(cDTO != null && cDTO.getMaxTeams() != null ? cDTO.getMaxTeams() : 500)
                .requireTeamAudit(cDTO != null && cDTO.getRequireTeamAudit() != null && cDTO.getRequireTeamAudit())
                .freezeScoreboardMinutes(cDTO != null && cDTO.getFreezeScoreboardMinutes() != null ? cDTO.getFreezeScoreboardMinutes() : 0)
                .isScoreboardFrozen(false)
                .allowWriteupSubmission(cDTO == null || cDTO.getAllowWriteupSubmission() == null || cDTO.getAllowWriteupSubmission())
                .build();
        configRepository.save(config);

        // Save Stages
        if (request.getStageConfigs() != null) {
            List<CompetitionStageConfig> stageEntities = new ArrayList<>();
            for (CompetitionStageConfigDTO sDTO : request.getStageConfigs()) {
                stageEntities.add(CompetitionStageConfig.builder()
                        .competition(savedComp)
                        .stage(sDTO.getStage())
                        .stageName(StringUtils.hasText(sDTO.getStageName()) ? sDTO.getStageName().trim() : sDTO.getStage().name())
                        .stageOrder(sDTO.getStageOrder() != null ? sDTO.getStageOrder() : 1)
                        .startTime(sDTO.getStartTime() != null ? sDTO.getStartTime() : savedComp.getStartTime())
                        .endTime(sDTO.getEndTime() != null ? sDTO.getEndTime() : savedComp.getEndTime())
                        .weightPercent(sDTO.getWeightPercent() != null ? sDTO.getWeightPercent() : 100.0)
                        .status(sDTO.getStatus() != null ? sDTO.getStatus() : StageStatus.PENDING)
                        .stageParamsJson(sDTO.getStageParamsJson())
                        .build());
            }
            stageConfigRepository.saveAll(stageEntities);
        }

        // Save Field Schemas
        if (request.getFieldSchemas() != null) {
            List<CompetitionFieldSchema> schemas = new ArrayList<>();
            for (CompetitionFieldSchemaDTO fDTO : request.getFieldSchemas()) {
                schemas.add(CompetitionFieldSchema.builder()
                        .competition(savedComp)
                        .fieldKey(fDTO.getFieldKey().trim())
                        .fieldLabel(fDTO.getFieldLabel().trim())
                        .fieldType(fDTO.getFieldType())
                        .isRequired(fDTO.getIsRequired() == null || fDTO.getIsRequired())
                        .regexPattern(fDTO.getRegexPattern())
                        .selectOptions(fDTO.getSelectOptions())
                        .fillScope(fDTO.getFillScope() != null ? fDTO.getFillScope() : FillScope.ALL_MEMBERS)
                        .sortOrder(fDTO.getSortOrder() != null ? fDTO.getSortOrder() : 0)
                        .build());
            }
            fieldSchemaRepository.saveAll(schemas);
        }

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

        if (request.getTitle() != null) comp.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) comp.setDescription(request.getDescription());
        if (request.getBannerUrl() != null) comp.setBannerUrl(request.getBannerUrl());
        if (request.getStatus() != null) comp.setStatus(request.getStatus());
        if (request.getRegStartTime() != null) comp.setRegStartTime(request.getRegStartTime());
        if (request.getRegEndTime() != null) comp.setRegEndTime(request.getRegEndTime());
        if (request.getStartTime() != null) comp.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) comp.setEndTime(request.getEndTime());

        competitionRepository.save(comp);

        // Update Config
        if (request.getConfig() != null) {
            CompetitionConfigDTO cDTO = request.getConfig();
            CompetitionConfig config = configRepository.findByCompetitionId(id)
                    .orElse(CompetitionConfig.builder().competition(comp).build());

            if (cDTO.getMaxTeamMembers() != null) config.setMaxTeamMembers(cDTO.getMaxTeamMembers());
            if (cDTO.getMinTeamMembers() != null) config.setMinTeamMembers(cDTO.getMinTeamMembers());
            if (cDTO.getMaxTeams() != null) config.setMaxTeams(cDTO.getMaxTeams());
            if (cDTO.getRequireTeamAudit() != null) config.setRequireTeamAudit(cDTO.getRequireTeamAudit());
            if (cDTO.getFreezeScoreboardMinutes() != null) config.setFreezeScoreboardMinutes(cDTO.getFreezeScoreboardMinutes());
            if (cDTO.getAllowWriteupSubmission() != null) config.setAllowWriteupSubmission(cDTO.getAllowWriteupSubmission());

            configRepository.save(config);
        }
    }

    @Override
    @Transactional
    public void deleteCompetition(Long id) {
        competitionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void setScoreboardFrozen(Long id, boolean frozen) {
        CompetitionConfig config = configRepository.findByCompetitionId(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛配置不存在"));
        config.setIsScoreboardFrozen(frozen);
        configRepository.save(config);
    }

}
