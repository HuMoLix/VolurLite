package com.humolix.volur.competition.service.ctf.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSubmission;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.SubmissionStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionChallengeRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository;
import com.humolix.volur.competition.service.ctf.CtfCompetitionService;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.CTF})
public class CtfCompetitionServiceImpl implements CtfCompetitionService {

    private final CompetitionMemberRepository memberRepository;
    private final CtfCompetitionChallengeRepository challengeRepository;
    private final CtfCompetitionSolvedRepository solvedRepository;
    private final CtfCompetitionSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CtfInstanceService containerInstanceService;
    private final CompetitionMapper competitionMapper;
    private final VolurCacheTemplate cacheTemplate;
    private final RedisOps redisOps;

    

    @Override
    public List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId) {
        // 1. 静态赛题列表底座：走三级缓存 L1 (Caffeine ~50ns) + L2 (Redis ~1ms) + L3 回源单飞折叠
        List<CtfCompetitionChallengeVO> cachedBaseList = cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "ctf:challenges:" + competitionId,
                () -> {
                    List<CtfCompetitionChallenge> challenges = challengeRepository
                            .findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc(competitionId);
                    return competitionMapper.toCtfChallengeVOList(challenges);
                }
        );

        if (cachedBaseList == null || cachedBaseList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 动态解题标记：查询当前队伍已解出的赛题 ID
        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        Set<Long> solvedChallengeIds = Collections.emptySet();
        if (myTeam != null) {
            solvedChallengeIds = solvedRepository.findByCompetitionIdAndTeamId(competitionId, myTeam.getId())
                    .stream()
                    .map(s -> s.getCompetitionChallenge().getId())
                    .collect(Collectors.toSet());
        }

        // 3. 利用 toBuilder 产生副本返回，防止修改污染 L1 本地缓存中的共享对象
        List<CtfCompetitionChallengeVO> result = new ArrayList<>(cachedBaseList.size());
        for (CtfCompetitionChallengeVO vo : cachedBaseList) {
            result.add(vo.toBuilder()
                    .isSolvedByMyTeam(solvedChallengeIds.contains(vo.getId()))
                    .build());
        }

        return result;
    }

    @Override
    public CtfCompetitionChallengeDetailVO getChallengeDetail(Long competitionId, Long competitionChallengeId, Long userId) {
        // 1. 静态赛题详情底座：三级缓存与单飞折叠，避免千人并发重复多表联查
        CtfCompetitionChallengeDetailVO cachedDetail = cacheTemplate.getOrLoad(
                CacheNames.CTF_CHALLENGE_DETAIL,
                "comp:" + competitionId + ":chal:" + competitionChallengeId,
                () -> {
                    CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

                    if (!compChallenge.getCompetition().getId().equals(competitionId) || !Boolean.TRUE.equals(compChallenge.getIsVisible())) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在或尚未公开");
                    }

                    CtfCompetitionChallengeDetailVO vo = competitionMapper.toCtfChallengeDetailVO(compChallenge);
                    CtfChallenge rawChallenge = compChallenge.getChallenge();
                    vo.setIsContainer(rawChallenge != null && rawChallenge.isDynamicContainer());
                    return vo;
                }
        );

        if (cachedDetail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在");
        }

        // 2. 动态判断当前战队解题状态
        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        boolean isSolved = myTeam != null && solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(
                competitionId, competitionChallengeId, myTeam.getId()
        );

        // 3. 不可变副本返回
        return cachedDetail.toBuilder()
                .isSolvedByMyTeam(isSolved)
                .build();
    }

    @Override
    @Transactional
    public boolean submitFlag(Long competitionId, Long competitionChallengeId, Long userId, CtfFlagSubmitRequestDTO request, String ip) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        if (!compChallenge.getCompetition().getId().equals(competitionId) || !Boolean.TRUE.equals(compChallenge.getIsVisible())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在");
        }

        Competition competition = compChallenge.getCompetition();

        // Check if team already solved this challenge
        if (solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(competitionId, competitionChallengeId, team.getId())) {
            recordSubmission(competition, compChallenge, team, userId, request.getFlag(), SubmissionStatus.DUPLICATE, ip);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您的队伍已经解出过该题目");
        }

        CtfChallenge challenge = compChallenge.getChallenge();
        boolean isCorrect = false;

        if (challenge.getType() == ChallengeType.STATIC) {
            isCorrect = StringUtils.hasText(challenge.getFlag()) && challenge.getFlag().trim().equalsIgnoreCase(request.getFlag().trim());
        } else {
            isCorrect = containerInstanceService.verifyFlag(challenge.getId(), userId, request.getFlag().trim());
        }

        if (!isCorrect) {
            recordSubmission(competition, compChallenge, team, userId, request.getFlag(), SubmissionStatus.WRONG, ip);
            return false;
        }

        // Record correct submission
        recordSubmission(competition, compChallenge, team, userId, request.getFlag(), SubmissionStatus.CORRECT, ip);

        // Update challenge solve count & calculate points
        int newSolvesCount = compChallenge.getSolvesCount() + 1;
        compChallenge.setSolvesCount(newSolvesCount);

        int newPoints = calculateDynamicPoints(
                compChallenge.getInitialPoints(),
                compChallenge.getMinPoints(),
                compChallenge.getDecayCount(),
                newSolvesCount
        );
        compChallenge.setCurrentPoints(newPoints);
        challengeRepository.save(compChallenge);

        boolean isFirstBlood = (newSolvesCount == 1);
        int pointsAwarded = isFirstBlood ? (int) (newPoints * 1.1) : newPoints;

        User user = userRepository.findById(userId).orElse(null);
        CtfCompetitionSolved solved = CtfCompetitionSolved.builder()
                .competition(competition)
                .competitionChallenge(compChallenge)
                .team(team)
                .user(user)
                .pointsAwarded(pointsAwarded)
                .isFirstBlood(isFirstBlood)
                .build();

        solvedRepository.save(solved);

        // Update points of previous solves for other teams to match new dynamic points
        List<CtfCompetitionSolved> allSolves = solvedRepository.findByCompetitionChallengeIdOrderBySolvedAtAsc(compChallenge.getId());
        for (CtfCompetitionSolved s : allSolves) {
            if (!s.getId().equals(solved.getId())) {
                s.setPointsAwarded(s.getIsFirstBlood() ? (int) (newPoints * 1.1) : newPoints);
                solvedRepository.save(s);
            }
        }

        // 解题成功后主动失效题目列表、题目详情与天梯榜物化缓存
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId);
        cacheTemplate.evict(CacheNames.CTF_CHALLENGE_DETAIL, "comp:" + competitionId + ":chal:" + competitionChallengeId);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:ctf:" + competitionId);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:composite:" + competitionId);

        return true;
    }

    @Override
    public ChallengeInstanceResponseVO startInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        Long stageId = 0L;
        String teamToken = null;

        // 同战队同赛题防并发重复拉起靶机容器分布式互斥锁
        String lockKey = "volur:lock:container:start:" + competitionId + ":" + competitionChallengeId + ":" + team.getId();
        try {
            return redisOps.withLock(lockKey, Duration.ofSeconds(3), Duration.ofSeconds(30), () ->
                    containerInstanceService.startInstance(
                            compChallenge.getChallenge().getId(),
                            userId,
                            stageId,
                            team.getId(),
                            teamToken
                    )
            );
        } catch (IllegalStateException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "靶机容器正在启动或排队中，请勿重复操作");
        }
    }

    @Override
    public void stopInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        containerInstanceService.stopInstance(compChallenge.getChallenge().getId(), userId);
    }

    @Override
    public ChallengeInstanceResponseVO extendInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        return containerInstanceService.extendInstance(compChallenge.getChallenge().getId(), userId);
    }

    private CompetitionTeam getMyApprovedTeamOrThrow(Long competitionId, Long userId) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }
        return team;
    }

    private int calculateDynamicPoints(int initialPoints, int minPoints, int decayCount, int solvesCount) {
        if (solvesCount <= 1) {
            return initialPoints;
        }
        double decay = Math.pow((double) (solvesCount - 1) / (double) Math.max(1, decayCount), 2);
        double points = minPoints + (initialPoints - minPoints) * (1.0 / (1.0 + decay));
        return (int) Math.round(Math.max(minPoints, points));
    }

    private void recordSubmission(Competition comp, CtfCompetitionChallenge chal, CompetitionTeam team, Long userId, String flag, SubmissionStatus status, String ip) {
        User user = userRepository.findById(userId).orElse(null);
        CtfCompetitionSubmission sub = CtfCompetitionSubmission.builder()
                .competition(comp)
                .competitionChallenge(chal)
                .team(team)
                .user(user)
                .submittedFlag(flag != null ? flag : "")
                .status(status)
                .ipAddress(ip)
                .build();
        submissionRepository.save(sub);
    }

    private CompetitionTeam getMyApprovedTeam(Long competitionId, Long userId) {
        if (userId == null) return null;
        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionTeam team = memberOpt.get().getTeam();
            if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                return team;
            }
        }
        return null;
    }

}
