package com.humolix.volur.competition.service.admin.impl;

import com.humolix.volur.competition.domain.dto.AdminCompetitionLogQueryDTO;
import com.humolix.volur.competition.domain.entity.CompetitionOperationLog;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogStatisticsVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogVO;
import com.humolix.volur.competition.mapper.CompetitionOperationLogMapper;
import com.humolix.volur.competition.repository.CompetitionOperationLogRepository;
import com.humolix.volur.competition.service.admin.AdminCompetitionLogService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCompetitionLogServiceImpl implements AdminCompetitionLogService {

    private final CompetitionOperationLogRepository logRepository;
    private final CompetitionOperationLogMapper logMapper;
    private final com.humolix.volur.competition.service.CompetitionLogPersistenceService logPersistenceService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminCompetitionLogVO> listLogs(Long competitionId, AdminCompetitionLogQueryDTO queryDTO) {
        if (competitionId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛 ID 不能为空");
        }

        // 读穿透强一致保证: 查询前先将 Redis 缓冲区中在途的当前微批日志刷盘，确保绝对实时性
        try {
            logPersistenceService.flushBuffer();
        } catch (Exception ignored) {
        }

        Pageable pageable = SortUtils.createPageable(
                queryDTO.getPage(),
                queryDTO.getSize(),
                CompetitionOperationLog.class,
                queryDTO.getSortBy(),
                queryDTO.getSortOrder(),
                "createdAt",
                Sort.Direction.DESC
        );

        Specification<CompetitionOperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 必须严格限定当前竞赛 ID
            predicates.add(cb.equal(root.get("competitionId"), competitionId));

            // 用户名模糊搜索
            if (StringUtils.hasText(queryDTO.getUsername())) {
                predicates.add(cb.like(root.get("username"), "%" + queryDTO.getUsername().trim() + "%"));
            }

            // 队伍名模糊搜索
            if (StringUtils.hasText(queryDTO.getTeamName())) {
                predicates.add(cb.like(root.get("teamName"), "%" + queryDTO.getTeamName().trim() + "%"));
            }

            // 题目名模糊搜索
            if (StringUtils.hasText(queryDTO.getChallengeTitle())) {
                predicates.add(cb.like(root.get("challengeTitle"), "%" + queryDTO.getChallengeTitle().trim() + "%"));
            }

            // 类别筛选
            if (StringUtils.hasText(queryDTO.getLogType())) {
                predicates.add(cb.equal(root.get("logType"), queryDTO.getLogType().trim().toUpperCase()));
            }

            // 状态筛选
            if (queryDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus()));
            }

            // 时间范围
            if (queryDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), queryDTO.getStartTime()));
            }

            if (queryDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), queryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CompetitionOperationLog> pageResult = logRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(logMapper::toVO));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCompetitionLogStatisticsVO getStatistics(Long competitionId) {
        if (competitionId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛 ID 不能为空");
        }

        try {
            logPersistenceService.flushBuffer();
        } catch (Exception ignored) {
        }

        long totalLogs = logRepository.countByCompetitionId(competitionId);
        long containerLogs = logRepository.countByCompetitionIdAndLogType(competitionId, "CONTAINER");
        long flagLogs = logRepository.countByCompetitionIdAndLogType(competitionId, "FLAG");
        long errorLogs = logRepository.countByCompetitionIdAndStatus(competitionId, false);

        return AdminCompetitionLogStatisticsVO.builder()
                .totalLogs(totalLogs)
                .containerLogs(containerLogs)
                .flagLogs(flagLogs)
                .errorLogs(errorLogs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCompetitionLogVO getLogDetail(Long competitionId, Long id) {
        if (competitionId == null || id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求参数不合法");
        }

        CompetitionOperationLog entity = logRepository.findByIdAndCompetitionId(id, competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛日志记录不存在或已清理"));

        return logMapper.toVO(entity);
    }

}
