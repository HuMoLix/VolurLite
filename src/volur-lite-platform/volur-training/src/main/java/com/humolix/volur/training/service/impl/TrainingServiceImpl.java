package com.humolix.volur.training.service.impl;

import com.humolix.volur.challenge.domain.dto.ctf.CtfAttachmentDTO;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.JsonUtils;
import com.humolix.volur.training.constant.TrainingConstants;
import com.humolix.volur.training.domain.entity.TrainingChallenge;
import com.humolix.volur.training.domain.entity.TrainingSolveRecord;
import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import com.humolix.volur.training.domain.vo.*;
import com.humolix.volur.training.repository.TrainingChallengeRepository;
import com.humolix.volur.training.repository.TrainingSolveRecordRepository;
import com.humolix.volur.training.service.TrainingRateLimitService;
import com.humolix.volur.training.service.TrainingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingChallengeRepository trainingChallengeRepository;
    private final TrainingSolveRecordRepository solveRecordRepository;
    private final CtfChallengeRepository ctfChallengeRepository;
    private final CtfInstanceService ctfInstanceService;
    private final TrainingRateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // ---------------------------------------------------------------------
    // 列表 / 详情
    // ---------------------------------------------------------------------

    @Override
    public PageResponse<TrainingChallengeListVO> listChallenges(int page, int size, Long categoryId, String difficulty,
                                                                String keyword, Long userId) {
        ChallengeDifficulty diff = parseDifficulty(difficulty);
        boolean hasSourceFilter = categoryId != null || diff != null || StringUtils.hasText(keyword);

        Specification<TrainingChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), TrainingChallengeStatus.ENABLED));
            if (hasSourceFilter) {
                // 题目名称/分类/难度在题库原题上：子查询在数据库侧过滤，避免全量载入内存
                var sq = query.subquery(Long.class);
                var c = sq.from(CtfChallenge.class);
                List<Predicate> sourcePredicates = new ArrayList<>();
                if (categoryId != null) {
                    sourcePredicates.add(cb.equal(c.get("category").get("id"), categoryId));
                }
                if (diff != null) {
                    sourcePredicates.add(cb.equal(c.get("difficulty"), diff));
                }
                if (StringUtils.hasText(keyword)) {
                    sourcePredicates.add(cb.like(c.get("name"), "%" + keyword.trim() + "%"));
                }
                sq.select(c.get("id")).where(sourcePredicates.toArray(new Predicate[0]));
                predicates.add(cb.in(root.get("sourceChallengeId")).value(sq));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TrainingChallenge> trainingPage = trainingChallengeRepository.findAll(spec, pageable);

        List<TrainingChallenge> trainings = trainingPage.getContent();
        Map<Long, CtfChallenge> sources = loadSources(trainings.stream()
                .map(TrainingChallenge::getSourceChallengeId).toList());
        Map<Long, Long> solveCounts = loadSolveCounts(trainings.stream()
                .map(TrainingChallenge::getSourceChallengeId).collect(Collectors.toSet()));
        Set<Long> solvedByMe = loadSolvedSourceIds(userId,
                trainings.stream().map(TrainingChallenge::getSourceChallengeId).collect(Collectors.toSet()));

        List<TrainingChallengeListVO> items = trainings.stream()
                .map(tc -> toListVO(tc, sources.get(tc.getSourceChallengeId()),
                        solveCounts.getOrDefault(tc.getSourceChallengeId(), 0L),
                        solvedByMe.contains(tc.getSourceChallengeId()), false))
                .toList();

        return PageResponse.of(items, trainingPage.getTotalElements(), trainingPage.getTotalPages(),
                Math.max(page, 1));
    }

    @Override
    public TrainingChallengeDetailVO getChallenge(Long id, Long userId) {
        TrainingChallenge training = requireEnabledTraining(id);
        CtfChallenge source = requireSource(training.getSourceChallengeId());
        TrainingSolveRecord myRecord = solveRecordRepository
                .findByUserIdAndSourceChallengeId(userId, training.getSourceChallengeId()).orElse(null);
        Map<Long, Long> solveCounts = loadSolveCounts(Set.of(training.getSourceChallengeId()));

        return TrainingChallengeDetailVO.builder()
                .id(training.getId())
                .sourceChallengeId(training.getSourceChallengeId())
                .name(source.getName())
                .categoryId(source.getCategory() != null ? source.getCategory().getId() : null)
                .categoryName(source.getCategory() != null ? source.getCategory().getName() : null)
                .difficulty(source.getDifficulty() != null ? source.getDifficulty().getDescription() : null)
                .points(training.getPoints())
                .solvedCount(solveCounts.getOrDefault(training.getSourceChallengeId(), 0L))
                .solvedByMe(myRecord != null)
                .solvedAt(myRecord != null ? myRecord.getSolvedAt() : null)
                .description(source.getDescription())
                .attachments(resolveAttachments(source))
                .dynamicContainer(source.isDynamicContainer())
                .build();
    }

    // ---------------------------------------------------------------------
    // flag 提交
    // ---------------------------------------------------------------------

    @Override
    public TrainingFlagResultVO submitFlag(Long id, Long userId, String flag) {
        TrainingChallenge training = requireEnabledTraining(id);
        CtfChallenge source = requireSource(training.getSourceChallengeId());

        rateLimitService.checkBeforeSubmit(userId, training.getSourceChallengeId());

        if (solveRecordRepository.existsByUserIdAndSourceChallengeId(userId, training.getSourceChallengeId())) {
            return TrainingFlagResultVO.builder()
                    .result(TrainingFlagResultVO.Result.DUPLICATE)
                    .pointsEarned(0)
                    .totalPoints(totalPointsOf(userId))
                    .build();
        }

        if (!isFlagCorrect(source, userId, flag)) {
            rateLimitService.recordWrongSubmit(userId, training.getSourceChallengeId());
            return TrainingFlagResultVO.builder()
                    .result(TrainingFlagResultVO.Result.WRONG)
                    .pointsEarned(0)
                    .totalPoints(totalPointsOf(userId))
                    .build();
        }

        try {
            // 故意不开事务：单条插入自身原子；若并发提交撞唯一约束，
            // 异常在本方法内爆发才能按 DUPLICATE 返回（开事务会在提交期爆发，500）
            solveRecordRepository.save(TrainingSolveRecord.builder()
                    .userId(userId)
                    .sourceChallengeId(training.getSourceChallengeId())
                    .pointsEarned(training.getPoints())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return TrainingFlagResultVO.builder()
                    .result(TrainingFlagResultVO.Result.DUPLICATE)
                    .pointsEarned(0)
                    .totalPoints(totalPointsOf(userId))
                    .build();
        }

        return TrainingFlagResultVO.builder()
                .result(TrainingFlagResultVO.Result.CORRECT)
                .pointsEarned(training.getPoints())
                .totalPoints(totalPointsOf(userId))
                .build();
    }

    private boolean isFlagCorrect(CtfChallenge source, Long userId, String submitted) {
        if (!StringUtils.hasText(submitted)) {
            return false;
        }
        if (source.getType() == ChallengeType.STATIC) {
            return StringUtils.hasText(source.getFlag())
                    && source.getFlag().trim().equalsIgnoreCase(submitted.trim());
        }
        // DYNAMIC / CONTAINER：与当前用户运行中靶机实例生成的动态 flag 比对
        return ctfInstanceService.verifyFlag(source.getId(), userId, submitted.trim());
    }

    // ---------------------------------------------------------------------
    // 靶机状态
    // ---------------------------------------------------------------------

    @Override
    public ChallengeInstanceResponseVO getInstance(Long id, Long userId) {
        TrainingChallenge training = requireEnabledTraining(id);
        requireSource(training.getSourceChallengeId());
        CtfChallengeInstance instance = ctfInstanceService
                .getInstance(training.getSourceChallengeId(), userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        TrainingConstants.EC_NO_RUNNING_INSTANCE, "当前没有运行中的靶机实例"));

        Instant now = Instant.now();
        long remaining = Math.max(Duration.between(now, instance.getExpiresAt()).getSeconds(), 0);
        return ChallengeInstanceResponseVO.builder()
                .challengeId(training.getSourceChallengeId())
                .accessUrl(instance.getAccessUrl())
                .status(instance.getStatus())
                .createdAt(instance.getCreatedAt())
                .expiresAt(instance.getExpiresAt())
                .remainingTimeSeconds((int) remaining)
                .build();
    }

    // ---------------------------------------------------------------------
    // 进度 / 排行榜
    // ---------------------------------------------------------------------

    @Override
    public TrainingProgressVO getProgress(Long userId) {
        List<TrainingChallenge> enabled = trainingChallengeRepository
                .findAll((root, query, cb) -> cb.equal(root.get("status"), TrainingChallengeStatus.ENABLED));
        Map<Long, CtfChallenge> sources = loadSources(
                enabled.stream().map(TrainingChallenge::getSourceChallengeId).toList());
        List<TrainingSolveRecord> myRecords = solveRecordRepository.findByUserIdAndSourceChallengeIdIn(
                userId, sources.keySet());

        Map<String, long[]> byCategory = new LinkedHashMap<>();
        for (TrainingChallenge training : enabled) {
            CtfChallenge source = sources.get(training.getSourceChallengeId());
            String category = source != null && source.getCategory() != null
                    ? source.getCategory().getName() : "未分类";
            byCategory.computeIfAbsent(category, k -> new long[2])[0]++;
        }
        Set<Long> mySolved = myRecords.stream()
                .map(TrainingSolveRecord::getSourceChallengeId).collect(Collectors.toSet());
        for (TrainingChallenge training : enabled) {
            if (mySolved.contains(training.getSourceChallengeId())) {
                CtfChallenge source = sources.get(training.getSourceChallengeId());
                String category = source != null && source.getCategory() != null
                        ? source.getCategory().getName() : "未分类";
                byCategory.computeIfAbsent(category, k -> new long[2])[1]++;
            }
        }

        long totalPoints = totalPointsOf(userId);
        List<AggRow> allRows = aggregateRows();
        TrainingLeaderboardVO.MyRank myRank = myRank(userId, allRows, visibleSortedRows(allRows));
        return TrainingProgressVO.builder()
                .totalChallenges(enabled.size())
                .solvedCount(mySolved.size())
                .totalPoints(totalPoints)
                .rank(myRank.getRank())
                .categories(byCategory.entrySet().stream()
                        .map(e -> new TrainingProgressVO.CategoryProgress(e.getKey(), e.getValue()[0], e.getValue()[1]))
                        .toList())
                .build();
    }

    @Override
    public List<TrainingCategoryVO> getCategories(Long userId) {
        List<TrainingChallenge> enabled = trainingChallengeRepository
                .findAll((root, query, cb) -> cb.equal(root.get("status"), TrainingChallengeStatus.ENABLED));
        Map<Long, CtfChallenge> sources = loadSources(
                enabled.stream().map(TrainingChallenge::getSourceChallengeId).toList());
        Set<Long> mySolved = solveRecordRepository.findByUserIdAndSourceChallengeIdIn(
                userId, sources.keySet()).stream()
                .map(TrainingSolveRecord::getSourceChallengeId)
                .collect(Collectors.toSet());

        Map<Long, TrainingCategoryVO> byCategoryId = new LinkedHashMap<>();
        for (TrainingChallenge training : enabled) {
            CtfChallenge source = sources.get(training.getSourceChallengeId());
            Long categoryId = source != null && source.getCategory() != null
                    ? source.getCategory().getId() : null;
            String name = source != null && source.getCategory() != null
                    ? source.getCategory().getName() : "未分类";
            TrainingCategoryVO vo = byCategoryId.computeIfAbsent(categoryId, key ->
                    TrainingCategoryVO.builder().id(key).name(name).total(0).solved(0).build());
            vo.setTotal(vo.getTotal() + 1);
            if (mySolved.contains(training.getSourceChallengeId())) {
                vo.setSolved(vo.getSolved() + 1);
            }
        }

        return byCategoryId.values().stream()
                .sorted(Comparator.comparingLong(TrainingCategoryVO::getTotal).reversed())
                .toList();
    }

    @Override
    public TrainingLeaderboardVO getLeaderboard(int limit, Long userId) {
        List<AggRow> allRows = aggregateRows();
        List<AggRow> visibleRows = visibleSortedRows(allRows);

        List<Long> userIds = visibleRows.stream().map(AggRow::userId).toList();
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, UserProfile> profiles = userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        int top = Math.min(Math.max(limit, 1), 100);
        List<TrainingLeaderboardVO.Item> items = new ArrayList<>();
        for (int i = 0; i < Math.min(top, visibleRows.size()); i++) {
            AggRow row = visibleRows.get(i);
            User user = users.get(row.userId());
            UserProfile profile = profiles.get(row.userId());
            items.add(TrainingLeaderboardVO.Item.builder()
                    .rank(i + 1)
                    .userId(row.userId())
                    .username(user != null ? user.getUsername() : String.valueOf(row.userId()))
                    .avatar(profile != null ? profile.getAvatarUrl() : null)
                    .points(row.points())
                    .solvedCount(row.solved())
                    .build());
        }

        TrainingLeaderboardVO.MyRank myRank = myRank(userId, allRows, visibleRows);
        return TrainingLeaderboardVO.builder().items(items).myRank(myRank).build();
    }

    private TrainingLeaderboardVO.MyRank myRank(Long userId, List<AggRow> allRows, List<AggRow> visibleSorted) {
        AggRow mine = allRows.stream()
                .filter(r -> r.userId().equals(userId)).findFirst().orElse(null);
        long points = mine != null ? mine.points() : 0;
        long solved = mine != null ? mine.solved() : 0;

        // 名次 = 可见榜上严格排在我之前的人数 + 1；榜外（无记录/隐藏）= 参与人数 + 1
        long rank;
        if (mine == null) {
            rank = solveRecordRepository.countParticipants(TrainingChallengeStatus.ENABLED) + 1;
        } else {
            AggRow finalMine = mine;
            rank = visibleSorted.stream().filter(r -> compareRows(r, finalMine) < 0).count() + 1;
        }
        return TrainingLeaderboardVO.MyRank.builder().rank(rank).points(points).solvedCount(solved).build();
    }

    /** 按积分 desc → 解出数 desc → 最后解出时间早者优先 排序，过滤隐藏用户 */
    private List<AggRow> visibleSortedRows(List<AggRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> hiddenUserIds = userRepository.findAllById(
                        rows.stream().map(AggRow::userId).toList()).stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsHidden()))
                .map(User::getId)
                .collect(Collectors.toSet());
        return rows.stream()
                .filter(r -> !hiddenUserIds.contains(r.userId()))
                .sorted(Comparator.comparingLong(AggRow::points).reversed()
                        .thenComparing(Comparator.comparingLong(AggRow::solved).reversed())
                        .thenComparing(AggRow::lastSolvedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<AggRow> aggregateRows() {
        return solveRecordRepository.aggregateLeaderboard(TrainingChallengeStatus.ENABLED).stream()
                .map(TrainingServiceImpl::toAggRow)
                .filter(Objects::nonNull)
                .toList();
    }

    private static AggRow toAggRow(Object[] row) {
        if (row == null || row.length < 4 || row[0] == null) {
            return null;
        }
        return new AggRow(
                (Long) row[0],
                row[1] instanceof Number n ? n.longValue() : 0L,
                row[2] instanceof Number n ? n.longValue() : 0L,
                row[3] instanceof Instant instant ? instant : null);
    }

    private static int compareRows(AggRow a, AggRow b) {
        int byPoints = Long.compare(b.points(), a.points());
        if (byPoints != 0) return byPoints;
        int bySolved = Long.compare(b.solved(), a.solved());
        if (bySolved != 0) return bySolved;
        if (a.lastSolvedAt() == null || b.lastSolvedAt() == null) {
            return a.lastSolvedAt() == null ? 1 : -1;
        }
        return a.lastSolvedAt().compareTo(b.lastSolvedAt());
    }

    private long totalPointsOf(Long userId) {
        List<Object[]> rows = solveRecordRepository.aggregateForUser(userId, TrainingChallengeStatus.ENABLED);
        if (rows.isEmpty() || rows.get(0).length < 2 || !(rows.get(0)[1] instanceof Number n)) {
            return 0L;
        }
        return n.longValue();
    }

    // ---------------------------------------------------------------------
    // 内部工具
    // ---------------------------------------------------------------------

    private TrainingChallenge requireEnabledTraining(Long id) {
        TrainingChallenge training = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        TrainingConstants.EC_TRAINING_CHALLENGE_NOT_FOUND, "训练题不存在"));
        if (training.getStatus() != TrainingChallengeStatus.ENABLED) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    TrainingConstants.EC_TRAINING_CHALLENGE_DISABLED, "该训练题已下架");
        }
        return training;
    }

    private CtfChallenge requireSource(Long sourceChallengeId) {
        return ctfChallengeRepository.findById(sourceChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        TrainingConstants.EC_TRAINING_CHALLENGE_NOT_FOUND, "训练题不存在"));
    }

    /** 难度筛选入参校验：接受英文值与中文别名，未知值拒绝（不静默回退 EASY） */
    private ChallengeDifficulty parseDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return null;
        }
        return ChallengeDifficulty.fromValueStrict(difficulty)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "难度参数不合法: " + difficulty));
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
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : solveRecordRepository.countSolvesBySourceChallengeIds(sourceIds)) {
            if (row[0] != null) {
                counts.put((Long) row[0], row[1] instanceof Number n ? n.longValue() : 0L);
            }
        }
        return counts;
    }

    private Set<Long> loadSolvedSourceIds(Long userId, Set<Long> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Set.of();
        }
        return solveRecordRepository.findByUserIdAndSourceChallengeIdIn(userId, sourceIds).stream()
                .map(TrainingSolveRecord::getSourceChallengeId)
                .collect(Collectors.toSet());
    }

    private TrainingChallengeListVO toListVO(TrainingChallenge training, CtfChallenge source, long solvedCount,
                                             boolean solvedByMe, boolean includeStatus) {
        boolean hasAttachment = source != null && (StringUtils.hasText(source.getAttachmentUrl())
                || StringUtils.hasText(source.getAttachments()));
        return TrainingChallengeListVO.builder()
                .id(training.getId())
                .sourceChallengeId(training.getSourceChallengeId())
                .name(source != null ? source.getName() : null)
                .categoryId(source != null && source.getCategory() != null ? source.getCategory().getId() : null)
                .categoryName(source != null && source.getCategory() != null ? source.getCategory().getName() : null)
                .difficulty(source != null && source.getDifficulty() != null
                        ? source.getDifficulty().getDescription() : null)
                .points(training.getPoints())
                .solvedCount(solvedCount)
                .solvedByMe(solvedByMe)
                .hasAttachment(hasAttachment)
                .dynamicContainer(source != null && source.isDynamicContainer())
                .status(includeStatus ? training.getStatus().name() : null)
                .build();
    }

    /** 多附件 JSON（attachments 字段）优先，兼容旧的单附件字段 */
    private List<TrainingAttachmentVO> resolveAttachments(CtfChallenge source) {
        List<TrainingAttachmentVO> result = new ArrayList<>();
        if (StringUtils.hasText(source.getAttachments())) {
            try {
                List<CtfAttachmentDTO> parsed = JsonUtils.parseList(source.getAttachments(), CtfAttachmentDTO.class);
                if (parsed != null) {
                    parsed.stream()
                            .filter(a -> a != null && StringUtils.hasText(a.getUrl()))
                            .map(a -> new TrainingAttachmentVO(
                                    StringUtils.hasText(a.getName()) ? a.getName() : "附件文件", a.getUrl()))
                            .forEach(result::add);
                }
            } catch (Exception ex) {
                log.warn("[Volur-Training] 附件 JSON 解析失败，回退单附件: challengeId={}", source.getId(), ex);
            }
        }
        if (result.isEmpty() && StringUtils.hasText(source.getAttachmentUrl())) {
            result.add(new TrainingAttachmentVO(
                    StringUtils.hasText(source.getAttachmentName()) ? source.getAttachmentName() : "附件文件",
                    source.getAttachmentUrl()));
        }
        return result;
    }

    private record AggRow(Long userId, long points, long solved, Instant lastSolvedAt) {
    }

}
