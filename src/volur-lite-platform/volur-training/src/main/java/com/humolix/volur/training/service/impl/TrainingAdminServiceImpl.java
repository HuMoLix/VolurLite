package com.humolix.volur.training.service.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.training.constant.TrainingConstants;
import com.humolix.volur.training.domain.dto.TrainingChallengeUpdateDTO;
import com.humolix.volur.training.domain.dto.TrainingImportRequestDTO;
import com.humolix.volur.training.domain.entity.TrainingChallenge;
import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import com.humolix.volur.training.domain.vo.TrainingChallengeListVO;
import com.humolix.volur.training.domain.vo.TrainingImportResultVO;
import com.humolix.volur.training.repository.TrainingChallengeRepository;
import com.humolix.volur.training.repository.TrainingSolveRecordRepository;
import com.humolix.volur.training.service.TrainingAdminService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingAdminServiceImpl implements TrainingAdminService {

    private final TrainingChallengeRepository trainingChallengeRepository;
    private final TrainingSolveRecordRepository solveRecordRepository;
    private final CtfChallengeRepository ctfChallengeRepository;

    @Override
    public PageResponse<TrainingChallengeListVO> listChallenges(int page, int size, String keyword, String status) {
        TrainingChallengeStatus parsedStatus = null;
        if (StringUtils.hasText(status)) {
            try {
                parsedStatus = TrainingChallengeStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "状态参数不合法: " + status);
            }
        }
        final TrainingChallengeStatus statusEnum = parsedStatus;
        final boolean hasKeyword = StringUtils.hasText(keyword);

        Specification<TrainingChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statusEnum != null) {
                predicates.add(cb.equal(root.get("status"), statusEnum));
            }
            if (hasKeyword) {
                // 按题库原题名称模糊搜索：子查询在数据库侧过滤，避免全量载入内存
                var sq = query.subquery(Long.class);
                var c = sq.from(CtfChallenge.class);
                sq.select(c.get("id")).where(cb.like(c.get("name"), "%" + keyword.trim() + "%"));
                predicates.add(cb.in(root.get("sourceChallengeId")).value(sq));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TrainingChallenge> trainingPage = trainingChallengeRepository.findAll(spec, pageable);

        List<TrainingChallenge> trainings = trainingPage.getContent();
        Map<Long, CtfChallenge> sources = loadSources(
                trainings.stream().map(TrainingChallenge::getSourceChallengeId).toList());
        Map<Long, Long> solveCounts = loadSolveCounts(
                trainings.stream().map(TrainingChallenge::getSourceChallengeId).collect(Collectors.toSet()));

        List<TrainingChallengeListVO> items = trainings.stream()
                .map(tc -> TrainingChallengeListVO.builder()
                        .id(tc.getId())
                        .sourceChallengeId(tc.getSourceChallengeId())
                        .name(sources.containsKey(tc.getSourceChallengeId())
                                ? sources.get(tc.getSourceChallengeId()).getName() : null)
                        .categoryName(sources.containsKey(tc.getSourceChallengeId())
                                && sources.get(tc.getSourceChallengeId()).getCategory() != null
                                ? sources.get(tc.getSourceChallengeId()).getCategory().getName() : null)
                        .difficulty(sources.containsKey(tc.getSourceChallengeId())
                                && sources.get(tc.getSourceChallengeId()).getDifficulty() != null
                                ? sources.get(tc.getSourceChallengeId()).getDifficulty().getDescription() : null)
                        .points(tc.getPoints())
                        .solvedCount(solveCounts.getOrDefault(tc.getSourceChallengeId(), 0L))
                        .hasAttachment(sources.containsKey(tc.getSourceChallengeId())
                                && (StringUtils.hasText(sources.get(tc.getSourceChallengeId()).getAttachmentUrl())
                                || StringUtils.hasText(sources.get(tc.getSourceChallengeId()).getAttachments())))
                        .dynamicContainer(sources.containsKey(tc.getSourceChallengeId())
                                && sources.get(tc.getSourceChallengeId()).isDynamicContainer())
                        .status(tc.getStatus().name())
                        .build())
                .toList();

        return PageResponse.of(items, trainingPage.getTotalElements(), trainingPage.getTotalPages(),
                Math.max(page, 1));
    }

    @Override
    public TrainingImportResultVO importChallenges(TrainingImportRequestDTO request, Long adminId) {
        Set<Long> requestedIds = new LinkedHashSet<>(request.getChallengeIds());

        Set<Long> alreadyInPool = trainingChallengeRepository
                .findBySourceChallengeIdIn(requestedIds).stream()
                .map(TrainingChallenge::getSourceChallengeId)
                .collect(Collectors.toSet());
        Map<Long, CtfChallenge> bank = ctfChallengeRepository.findAllById(requestedIds).stream()
                .collect(Collectors.toMap(CtfChallenge::getId, c -> c));

        List<TrainingChallenge> toCreate = new ArrayList<>();
        List<TrainingImportResultVO.Skipped> skipped = new ArrayList<>();
        for (Long challengeId : requestedIds) {
            if (alreadyInPool.contains(challengeId)) {
                skipped.add(TrainingImportResultVO.Skipped.builder()
                        .challengeId(challengeId)
                        .reason(TrainingImportResultVO.SkipReason.ALREADY_IN_POOL)
                        .build());
            } else if (!bank.containsKey(challengeId)) {
                skipped.add(TrainingImportResultVO.Skipped.builder()
                        .challengeId(challengeId)
                        .reason(TrainingImportResultVO.SkipReason.NOT_FOUND)
                        .build());
            } else {
                CtfChallenge source = bank.get(challengeId);
                toCreate.add(TrainingChallenge.builder()
                        .sourceChallengeId(challengeId)
                        .points(defaultPoints(source))
                        .status(TrainingChallengeStatus.ENABLED)
                        .createdBy(adminId)
                        .build());
            }
        }

        // 逐行独立保存（不开事务）：并发导入撞唯一约束时异常在本方法内爆发，
        // 该题按 ALREADY_IN_POOL 返回而非 500，其余题目正常导入
        List<TrainingChallenge> created = new ArrayList<>();
        for (TrainingChallenge tc : toCreate) {
            try {
                created.add(trainingChallengeRepository.save(tc));
            } catch (DataIntegrityViolationException ex) {
                skipped.add(TrainingImportResultVO.Skipped.builder()
                        .challengeId(tc.getSourceChallengeId())
                        .reason(TrainingImportResultVO.SkipReason.ALREADY_IN_POOL)
                        .build());
            }
        }

        return TrainingImportResultVO.builder()
                .imported(created.size())
                .skipped(skipped)
                .build();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, TrainingChallengeUpdateDTO request) {
        TrainingChallenge training = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        TrainingConstants.EC_TRAINING_CHALLENGE_NOT_FOUND, "训练题不存在"));
        if (request.getPoints() != null) {
            training.setPoints(request.getPoints());
        }
        if (request.getStatus() != null) {
            training.setStatus(request.getStatus());
        }
        trainingChallengeRepository.save(training);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        TrainingChallenge training = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        TrainingConstants.EC_TRAINING_CHALLENGE_NOT_FOUND, "训练题不存在"));
        // 只移出训练池，解题记录保留：同题重新导入后已解状态与解出数自动恢复
        trainingChallengeRepository.delete(training);
    }

    private int defaultPoints(CtfChallenge source) {
        Integer points = TrainingConstants.DEFAULT_POINTS_BY_DIFFICULTY.get(
                source.getDifficulty() != null ? source.getDifficulty()
                        : com.humolix.volur.challenge.domain.enums.ChallengeDifficulty.EASY);
        return points != null ? points : 100;
    }

    private Map<Long, CtfChallenge> loadSources(List<Long> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return ctfChallengeRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(CtfChallenge::getId, c -> c));
    }

    private Map<Long, Long> loadSolveCounts(Set<Long> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new java.util.HashMap<>();
        for (Object[] row : solveRecordRepository.countSolvesBySourceChallengeIds(sourceIds)) {
            if (row[0] != null) {
                counts.put((Long) row[0], row[1] instanceof Number n ? n.longValue() : 0L);
            }
        }
        return counts;
    }

}
