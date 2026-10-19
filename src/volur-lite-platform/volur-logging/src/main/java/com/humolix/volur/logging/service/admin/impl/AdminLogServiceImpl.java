package com.humolix.volur.logging.service.admin.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.logging.domain.dto.AdminLogQueryDTO;
import com.humolix.volur.logging.domain.entity.Syslog;
import com.humolix.volur.logging.domain.vo.AdminLogStatisticsVO;
import com.humolix.volur.logging.domain.vo.AdminLogVO;
import com.humolix.volur.logging.mapper.SyslogMapper;
import com.humolix.volur.logging.repository.SyslogRepository;
import com.humolix.volur.logging.service.admin.AdminLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final SyslogRepository syslogRepository;
    private final SyslogMapper syslogMapper;
    private final com.humolix.volur.logging.service.SyslogPersistenceService syslogPersistenceService;

    @Override
    public PageResponse<AdminLogVO> listLogs(AdminLogQueryDTO queryDTO) {
        try {
            syslogPersistenceService.flushBuffer();
        } catch (Exception ignored) {
        }
        Pageable pageable = SortUtils.createPageable(
                queryDTO.getPage(),
                queryDTO.getSize(),
                Syslog.class,
                queryDTO.getSortBy(),
                queryDTO.getSortOrder(),
                "createdAt",
                Sort.Direction.DESC
        );

        Specification<Syslog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(queryDTO.getKeyword())) {
                String keywordLike = "%" + queryDTO.getKeyword().trim() + "%";
                Predicate operLike = cb.like(root.get("operator"), keywordLike);
                Predicate ipLike = cb.like(root.get("operatorIp"), keywordLike);
                Predicate actionLike = cb.like(root.get("action"), keywordLike);
                Predicate uriLike = cb.like(root.get("requestUri"), keywordLike);
                predicates.add(cb.or(operLike, ipLike, actionLike, uriLike));
            }

            if (StringUtils.hasText(queryDTO.getLevel())) {
                predicates.add(cb.equal(root.get("level"), queryDTO.getLevel().trim().toUpperCase()));
            }

            if (StringUtils.hasText(queryDTO.getModule())) {
                predicates.add(cb.equal(root.get("module"), queryDTO.getModule().trim().toUpperCase()));
            }

            if (queryDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus()));
            }

            if (queryDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), queryDTO.getStartTime()));
            }

            if (queryDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), queryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Syslog> entityPage = syslogRepository.findAll(spec, pageable);
        return PageResponse.of(entityPage.map(syslogMapper::toVO));
    }

    @Override
    public AdminLogStatisticsVO getStatistics() {
        try {
            syslogPersistenceService.flushBuffer();
        } catch (Exception ignored) {
        }
        long totalLogs = syslogRepository.count();
        long authLogs = syslogRepository.countByModuleOrLogType("AUTH", "AUTH");
        long securityLogs = syslogRepository.countByLogType("SECURITY");
        long errorLogs = syslogRepository.countByLevel("ERROR");

        return AdminLogStatisticsVO.builder()
                .totalLogs(totalLogs)
                .authLogs(authLogs)
                .securityLogs(securityLogs)
                .errorLogs(errorLogs)
                .build();
    }

    @Override
    public AdminLogVO getLogDetail(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "日志 ID 不能为空");
        }

        Syslog syslog = syslogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "日志记录不存在或已清理"));

        return syslogMapper.toVO(syslog);
    }

}
