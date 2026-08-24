package com.humolix.volur.challenge.service.stage.impl;

import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.challenge.entity.Solved;
import com.humolix.volur.challenge.entity.Submission;
import com.humolix.volur.challenge.entity.enums.ChallengeType;
import com.humolix.volur.challenge.entity.enums.SceneType;
import com.humolix.volur.challenge.entity.enums.SubmissionStatus;
import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.challenge.entity.stage.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.entity.stage.ctf.CtfTrainingChallenge;
import com.humolix.volur.challenge.object.dto.FlagSubmissionRequestDTO;
import com.humolix.volur.challenge.object.vo.pub.AttachmentVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengePageResponseVO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfTrainingChallengeSolverResponseVO;
import com.humolix.volur.challenge.repository.SolvedRepository;
import com.humolix.volur.challenge.repository.SubmissionRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfTrainingChallengeRepository;
import com.humolix.volur.challenge.service.provider.ChallengeContainerProvider;
import com.humolix.volur.challenge.service.stage.CtfChallengeService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.framework.security.util.RequestHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CtfChallengeServiceImpl implements CtfChallengeService {

    private final CtfChallengeCategoryRepository categoryRepository;
    private final CtfTrainingChallengeRepository trainingChallengeRepository;
    private final SolvedRepository solvedRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final Optional<ChallengeContainerProvider> containerProvider;

    @Override
    public List<CtfChallengeCategoryResponseVO> listCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CtfChallengeCategoryResponseVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .iconUrl(c.getIconUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<CtfTrainingChallengePageResponseVO> pageTrainingChallenges(Integer page, Integer size, Long categoryId, String keyword, Long userId) {
        Pageable pageable = SortUtils.createPageable(page, size);
        Specification<CtfTrainingChallenge> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.isTrue(root.get("active")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("challenge").get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("challenge").get("name"), "%" + keyword + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<CtfTrainingChallenge> pageResult = trainingChallengeRepository.findAll(spec, pageable);
        if (pageResult.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> challengeIds = pageResult.getContent().stream()
                .map(tc -> tc.getChallenge().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> solvedIds = new HashSet<>();
        if (userId != null && !challengeIds.isEmpty()) {
            List<Long> solvedList = solvedRepository.findSolvedChallengeIds(userId, SceneType.TRAINING, 0L, challengeIds);
            if (solvedList != null) {
                solvedIds.addAll(solvedList);
            }
        }

        final Set<Long> finalSolvedIds = solvedIds;
        return pageResult.map(tc -> {
            CtfChallenge c = tc.getChallenge();
            boolean isSolved = finalSolvedIds.contains(c.getId());
            return CtfTrainingChallengePageResponseVO.builder()
                    .id(tc.getId())
                    .name(c.getName())
                    .category(c.getCategory() != null ? c.getCategory().getName() : null)
                    .points(tc.getCurrentPoints())
                    .solvedCount(tc.getSolveCount())
                    .difficulty(c.getDifficulty())
                    .solved(isSolved)
                    .dynamicContainer(c.isDynamicContainer())
                    .build();
        });
    }

    @Override
    public CtfTrainingChallengeDetailResponseVO getTrainingChallengeDetail(Long trainingChallengeId, Long userId) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findWithChallengeById(trainingChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "训练场题目不存在"));

        CtfChallenge c = tc.getChallenge();
        boolean isSolved = (userId != null) && Boolean.TRUE.equals(
                solvedRepository.existsByUserIdAndSceneTypeAndSceneIdAndChallengeId(userId, SceneType.TRAINING, 0L, c.getId())
        );

        List<AttachmentVO> attachmentVOs = new ArrayList<>();
        if (c.getAttachments() != null) {
            c.getAttachments().forEach(att -> attachmentVOs.add(
                    AttachmentVO.builder()
                            .fileName(att.getFileName())
                            .fileUrl(att.getFileUrl())
                            .fileExtension(att.getFileCheckSum())
                            .build()
            ));
        }

        String containerUrl = null;
        Integer remainingSeconds = null;
        if (c.isDynamicContainer() && userId != null && containerProvider.isPresent()) {
            containerUrl = containerProvider.get().getAccessUrl(c.getId(), userId);
            remainingSeconds = containerProvider.get().getRemainingSeconds(c.getId(), userId);
        }

        return CtfTrainingChallengeDetailResponseVO.builder()
                .id(tc.getId())
                .name(c.getName())
                .description(c.getDescription())
                .category(c.getCategory() != null ? c.getCategory().getName() : null)
                .points(tc.getCurrentPoints())
                .solvedCount(tc.getSolveCount())
                .difficulty(c.getDifficulty())
                .solved(isSolved)
                .dynamicContainer(c.isDynamicContainer())
                .containerUrl(containerUrl)
                .remainingTimeSeconds(remainingSeconds)
                .attachments(attachmentVOs)
                .build();
    }

    @Override
    public Page<CtfTrainingChallengeSolverResponseVO> pageSolvers(Long trainingChallengeId, Integer page, Integer size) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findWithChallengeById(trainingChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "训练场题目不存在"));

        Pageable pageable = SortUtils.createPageable(page, size);
        Page<Solved> solves = solvedRepository.findByChallengeIdAndSceneTypeAndSceneIdOrderBySolvedAtAsc(
                tc.getChallenge().getId(), SceneType.TRAINING, 0L, pageable
        );

        if (solves.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> userIds = solves.getContent().stream().map(Solved::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return solves.map(solve -> {
            User u = userMap.get(solve.getUserId());
            return CtfTrainingChallengeSolverResponseVO.builder()
                    .userId(solve.getUserId())
                    .username(u != null ? u.getUsername() : "未知用户")
                    .avatarUrl(u != null && u.getUserProfile() != null ? u.getUserProfile().getAvatarUrl() : null)
                    .solvedAt(solve.getSolvedAt())
                    .build();
        });
    }

    @Override
    @Transactional
    public boolean submitFlag(Long trainingChallengeId, FlagSubmissionRequestDTO request, Long userId) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findWithChallengeById(trainingChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        CtfChallenge challenge = tc.getChallenge();
        String submittedFlag = request.getFlag() != null ? request.getFlag().trim() : "";

        boolean alreadySolved = Boolean.TRUE.equals(
                solvedRepository.existsByUserIdAndSceneTypeAndSceneIdAndChallengeId(userId, SceneType.TRAINING, 0L, challenge.getId())
        );

        if (alreadySolved) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您已经解出此题，无需重复提交");
        }

        boolean isCorrect = false;
        if (challenge.isDynamicContainer()) {
            if (containerProvider.isPresent()) {
                isCorrect = containerProvider.get().verifyFlag(challenge.getId(), userId, submittedFlag);
            }
        } else {
            if (challenge.getFlag() != null && challenge.getFlag().trim().equalsIgnoreCase(submittedFlag)) {
                isCorrect = true;
            }
        }

        String ip = RequestHolder.getClientIp();
        String ua = RequestHolder.getUserAgent();
        int earnedPoints = isCorrect ? tc.getCurrentPoints() : 0;

        Submission submission = Submission.builder()
                .userId(userId)
                .challengeId(challenge.getId())
                .challengeType(ChallengeType.CTF)
                .sceneType(SceneType.TRAINING)
                .sceneId(0L)
                .content(submittedFlag)
                .status(isCorrect ? SubmissionStatus.SUCCESS : SubmissionStatus.FAILED)
                .score(earnedPoints)
                .ipAddress(ip != null ? ip : "127.0.0.1")
                .userAgent(ua != null ? ua : "")
                .build();
        submissionRepository.save(submission);

        if (isCorrect) {
            Solved solved = Solved.builder()
                    .userId(userId)
                    .challengeId(challenge.getId())
                    .challengeType(ChallengeType.CTF)
                    .sceneType(SceneType.TRAINING)
                    .sceneId(0L)
                    .score(earnedPoints)
                    .build();
            solvedRepository.save(solved);

            // Update user stats (points & solves on User)
            userRepository.findById(userId).ifPresent(u -> {
                u.setSolves((u.getSolves() != null ? u.getSolves() : 0L) + 1);
                u.setTotalPoints((u.getTotalPoints() != null ? u.getTotalPoints() : 0.0) + earnedPoints);
                userRepository.save(u);
            });

            // Increment challenge solve count
            tc.setSolveCount(tc.getSolveCount() + 1);

            // Dynamic score recalculation
            if (tc.isDynamicScore()) {
                int solveCount = tc.getSolveCount();
                float decay = tc.getDecayCoefficient() != null ? tc.getDecayCoefficient() : 0.5f;
                int initial = tc.getInitialPoints();
                int min = tc.getMinimumPoints();
                int newPoints = (int) Math.max(min, initial * (decay / (decay + solveCount)));
                tc.setCurrentPoints(newPoints);
            }
            trainingChallengeRepository.save(tc);
        }

        return isCorrect;
    }

}
