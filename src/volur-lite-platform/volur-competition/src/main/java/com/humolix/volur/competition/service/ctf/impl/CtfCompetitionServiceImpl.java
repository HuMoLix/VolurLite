package com.humolix.volur.competition.service.ctf.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSubmission;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.SubmissionStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.ctf.CtfChallengeSolveVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfSolveNotificationVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfTeamSolveRecordVO;
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
import com.humolix.volur.notification.domain.enums.RealtimeEventType;
import com.humolix.volur.notification.service.RealtimePushService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.CTF})
@Transactional(readOnly = true)
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
    private final RealtimePushService realtimePushService;
    private final ObjectMapper objectMapper;

    

    @Override
    public List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
        List<CtfCompetitionChallengeVO> cachedBaseList = cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "ctf:challenges:" + competitionId,
                () -> {
                    List<CtfCompetitionChallenge> challenges = challengeRepository
                            .findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc(competitionId);
                    List<CtfCompetitionSolved> allSolves = solvedRepository.findAllSolvesWithDetails(competitionId);
                    Map<Long, List<String>> challengeTopTeamsMap = new HashMap<>();
                    for (CtfCompetitionSolved s : allSolves) {
                        if (s.getCompetitionChallenge() != null && s.getTeam() != null) {
                            Long chalId = s.getCompetitionChallenge().getId();
                            List<String> teams = challengeTopTeamsMap.computeIfAbsent(chalId, k -> new ArrayList<>());
                            if (teams.size() < 3 && StringUtils.hasText(s.getTeam().getTeamName())) {
                                teams.add(s.getTeam().getTeamName());
                            }
                        }
                    }

                    List<CtfCompetitionChallengeVO> voList = new ArrayList<>(challenges.size());
                    for (CtfCompetitionChallenge c : challenges) {
                        CtfCompetitionChallengeVO vo = competitionMapper.toCtfChallengeVO(c);
                        vo.setTopTeams(challengeTopTeamsMap.getOrDefault(c.getId(), Collections.emptyList()));
                        voList.add(vo);
                    }
                    return voList;
                }
        );

        if (cachedBaseList == null || cachedBaseList.isEmpty()) {
            return Collections.emptyList();
        }

        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        Set<Long> solvedChallengeIds = Collections.emptySet();
        if (myTeam != null) {
            solvedChallengeIds = solvedRepository.findByCompetitionIdAndTeamId(competitionId, myTeam.getId())
                    .stream()
                    .map(s -> s.getCompetitionChallenge().getId())
                    .collect(Collectors.toSet());
        }

        List<CtfCompetitionChallengeVO> result = new ArrayList<>(cachedBaseList.size());
        for (Object item : cachedBaseList) {
            CtfCompetitionChallengeVO vo = item instanceof CtfCompetitionChallengeVO cvo
                    ? cvo
                    : objectMapper.convertValue(item, CtfCompetitionChallengeVO.class);
            result.add(vo.toBuilder()
                    .isSolvedByMyTeam(solvedChallengeIds.contains(vo.getId()))
                    .build());
        }

        return result;
    }

    @Override
    public CtfCompetitionChallengeDetailVO getChallengeDetail(Long competitionId, Long competitionChallengeId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
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

                    List<CtfCompetitionSolved> solves = solvedRepository.findByCompetitionChallengeIdWithDetails(competitionChallengeId);
                    List<CtfChallengeSolveVO> solveVOList = new ArrayList<>(solves.size());
                    List<String> topTeams = new ArrayList<>();
                    for (int i = 0; i < solves.size(); i++) {
                        CtfCompetitionSolved s = solves.get(i);
                        String tName = s.getTeam() != null ? s.getTeam().getTeamName() : "";
                        String aUrl = s.getTeam() != null ? s.getTeam().getAvatarUrl() : "";
                        solveVOList.add(CtfChallengeSolveVO.builder()
                                .teamId(s.getTeam() != null ? s.getTeam().getId() : null)
                                .teamName(tName)
                                .avatarUrl(aUrl)
                                .solvedAt(s.getSolvedAt())
                                .points(s.getPointsAwarded())
                                .isFirstBlood(Boolean.TRUE.equals(s.getIsFirstBlood()) || i == 0)
                                .build());
                        if (topTeams.size() < 3 && StringUtils.hasText(tName)) {
                            topTeams.add(tName);
                        }
                    }
                    vo.setSolves(solveVOList);
                    vo.setTopTeams(topTeams);

                    return vo;
                }
        );

        if (cachedDetail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在");
        }

        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        boolean isSolved = myTeam != null && solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(
                competitionId, competitionChallengeId, myTeam.getId()
        );

        ChallengeInstanceResponseVO instanceVO = null;
        if (Boolean.TRUE.equals(cachedDetail.getIsContainer())) {
            List<Long> memberUserIds = Collections.emptyList();
            Long teamId = null;
            if (myTeam != null) {
                teamId = myTeam.getId();
                List<CompetitionMember> members = memberRepository.findByTeamId(teamId);
                memberUserIds = members.stream()
                        .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                        .filter(Objects::nonNull)
                        .toList();
            } else if (userId != null) {
                memberUserIds = List.of(userId);
            }
            ChallengeInstanceResponseVO rawInstVO = containerInstanceService.getTeamInstance(
                    competitionId, teamId, cachedDetail.getChallengeId(), memberUserIds
            );
            if (rawInstVO != null) {
                instanceVO = rawInstVO.toBuilder()
                        .challengeId(competitionChallengeId)
                        .build();
            }
        }

        return cachedDetail.toBuilder()
                .isSolvedByMyTeam(isSolved)
                .instance(instanceVO)
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

        if (solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(competitionId, competitionChallengeId, team.getId())) {
            recordSubmission(competition, compChallenge, team, userId, request.getFlag(), SubmissionStatus.DUPLICATE, ip);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您的队伍已经解出过该题目");
        }

        CtfChallenge challenge = compChallenge.getChallenge();
        boolean isCorrect = false;

        if (challenge.getType() == ChallengeType.STATIC) {
            isCorrect = StringUtils.hasText(challenge.getFlag()) && challenge.getFlag().trim().equalsIgnoreCase(request.getFlag().trim());
        } else {
            List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
            List<Long> memberUserIds = members.stream()
                    .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                    .filter(Objects::nonNull)
                    .toList();
            isCorrect = containerInstanceService.verifyTeamFlag(
                    competitionId, team.getId(), challenge.getId(), memberUserIds, request.getFlag().trim()
            );
        }

        if (!isCorrect) {
            recordSubmission(competition, compChallenge, team, userId, request.getFlag(), SubmissionStatus.WRONG, ip);
            return false;
        }

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
        boolean isSecondBlood = (newSolvesCount == 2);
        boolean isThirdBlood = (newSolvesCount == 3);

        CompetitionConfig config = competition.getConfig();
        boolean enableBloodBonus = config != null && Boolean.TRUE.equals(config.getEnableBloodBonus());
        int bonus = 0;
        if (enableBloodBonus) {
            int pct = 0;
            if (isFirstBlood) {
                pct = config.getFirstBloodBonusPercentage() != null ? config.getFirstBloodBonusPercentage() : 10;
            } else if (isSecondBlood) {
                pct = config.getSecondBloodBonusPercentage() != null ? config.getSecondBloodBonusPercentage() : 5;
            } else if (isThirdBlood) {
                pct = config.getThirdBloodBonusPercentage() != null ? config.getThirdBloodBonusPercentage() : 2;
            }
            if (pct > 0) {
                bonus = (int) Math.round(newPoints * (pct / 100.0));
            }
        }
        int pointsAwarded = newPoints + bonus;

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CtfCompetitionSolved solved = CtfCompetitionSolved.builder()
                .competition(competition)
                .competitionChallenge(compChallenge)
                .team(team)
                .user(user)
                .pointsAwarded(pointsAwarded)
                .isFirstBlood(isFirstBlood)
                .build();

        solvedRepository.save(solved);

        List<CtfCompetitionSolved> allSolves = solvedRepository.findByCompetitionChallengeIdOrderBySolvedAtAsc(compChallenge.getId());
        for (int i = 0; i < allSolves.size(); i++) {
            CtfCompetitionSolved s = allSolves.get(i);
            int itemBonus = 0;
            if (enableBloodBonus) {
                int pct = 0;
                if (i == 0) pct = config.getFirstBloodBonusPercentage() != null ? config.getFirstBloodBonusPercentage() : 10;
                else if (i == 1) pct = config.getSecondBloodBonusPercentage() != null ? config.getSecondBloodBonusPercentage() : 5;
                else if (i == 2) pct = config.getThirdBloodBonusPercentage() != null ? config.getThirdBloodBonusPercentage() : 2;
                if (pct > 0) {
                    itemBonus = (int) Math.round(newPoints * (pct / 100.0));
                }
            }
            s.setPointsAwarded(newPoints + itemBonus);
            solvedRepository.save(s);
        }

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId);
        cacheTemplate.evict(CacheNames.CTF_CHALLENGE_DETAIL, "comp:" + competitionId + ":chal:" + competitionChallengeId);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:ctf:" + competitionId);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "scoreboard:composite:" + competitionId);

        String bloodType = switch (newSolvesCount) {
            case 1 -> "FIRST_BLOOD";
            case 2 -> "SECOND_BLOOD";
            case 3 -> "THIRD_BLOOD";
            default -> "NORMAL";
        };

        String challengeTitle = compChallenge.getChallenge() != null ? compChallenge.getChallenge().getName() : "";
        String category = compChallenge.getChallenge() != null && compChallenge.getChallenge().getCategory() != null
                ? compChallenge.getChallenge().getCategory().getName()
                : "";

        CtfSolveNotificationVO notificationVO = CtfSolveNotificationVO.builder()
                .competitionId(competitionId)
                .challengeId(compChallenge.getId())
                .challengeTitle(challengeTitle)
                .challengeCategory(category)
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .teamAvatar(team.getAvatarUrl())
                .userId(userId)
                .username(user != null ? user.getUsername() : null)
                .userNickname(user != null ? user.getRealName() : null)
                .solveRank(newSolvesCount)
                .bloodType(bloodType)
                .pointsAwarded(pointsAwarded)
                .solvedAt(Instant.now())
                .build();

        realtimePushService.broadcastAll(RealtimeEventType.COMPETITION_EVENT.getValue(), notificationVO);

        return true;
    }

    @Override
    public ChallengeInstanceResponseVO startInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        if (!compChallenge.getCompetition().getId().equals(competitionId) || !Boolean.TRUE.equals(compChallenge.getIsVisible())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在或尚未公开");
        }

        CtfChallenge challenge = compChallenge.getChallenge();

        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        List<Long> memberUserIds = members.stream()
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(Objects::nonNull)
                .toList();

        ChallengeInstanceResponseVO existingVO = containerInstanceService.getTeamInstance(
                competitionId, team.getId(), challenge.getId(), memberUserIds
        );
        if (existingVO != null) {
            return existingVO.toBuilder()
                    .challengeId(competitionChallengeId)
                    .build();
        }

        Competition competition = compChallenge.getCompetition();
        CompetitionConfig config = competition.getConfig();
        int maxQuota = (config != null && config.getMaxContainersPerTeam() != null) ? config.getMaxContainersPerTeam() : 2;
        long activeCount = containerInstanceService.countActiveInstancesForTeam(
                competitionId, team.getId(), memberUserIds
        );
        if (activeCount >= maxQuota) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "本场比赛单支队伍在同一时刻最多允许存在 " + maxQuota + " 个容器，请先销毁已有容器后重试");
        }

        String teamToken = null;
        String lockKey = "volur:lock:container:start:" + competitionId + ":" + competitionChallengeId + ":" + team.getId();
        try {
            return redisOps.withLock(lockKey, Duration.ofSeconds(3), Duration.ofSeconds(30), () -> {
                ChallengeInstanceResponseVO vo = containerInstanceService.startInstance(
                        challenge.getId(),
                        userId,
                        competitionId,
                        team.getId(),
                        teamToken
                );
                return vo != null ? vo.toBuilder().challengeId(competitionChallengeId).build() : null;
            });
        } catch (IllegalStateException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "靶机容器正在启动或排队中，请勿重复操作");
        }
    }

    @Override
    public void stopInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        List<Long> memberUserIds = members.stream()
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(Objects::nonNull)
                .toList();

        containerInstanceService.stopTeamInstance(
                competitionId, team.getId(), compChallenge.getChallenge().getId(), memberUserIds
        );
    }

    @Override
    public ChallengeInstanceResponseVO extendInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        List<Long> memberUserIds = members.stream()
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(Objects::nonNull)
                .toList();

        ChallengeInstanceResponseVO vo = containerInstanceService.extendTeamInstance(
                competitionId, team.getId(), compChallenge.getChallenge().getId(), memberUserIds
        );
        return vo != null ? vo.toBuilder().challengeId(competitionChallengeId).build() : null;
    }

    @Override
    public ChallengeInstanceResponseVO getInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        List<Long> memberUserIds = members.stream()
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(Objects::nonNull)
                .toList();

        ChallengeInstanceResponseVO vo = containerInstanceService.getTeamInstance(
                competitionId, team.getId(), compChallenge.getChallenge().getId(), memberUserIds
        );
        return vo != null ? vo.toBuilder().challengeId(competitionChallengeId).build() : null;
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
            if (team != null && team.getStatus() == TeamStatus.BANNED) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
            }
            if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                return team;
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
            CompetitionTeam team = memberOpt.get().getTeam();
            if (team != null && team.getStatus() == TeamStatus.BANNED) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
            }
            if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                return;
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        boolean isAdmin = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        if (!isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }
    }

    @Override
    public List<CtfTeamSolveRecordVO> getTeamSolves(Long competitionId, Long teamId) {
        List<CtfCompetitionSolved> solves = solvedRepository.findByCompetitionIdAndTeamIdWithDetails(competitionId, teamId);
        List<CtfTeamSolveRecordVO> result = new ArrayList<>(solves.size());
        for (CtfCompetitionSolved s : solves) {
            boolean isContainer = s.getCompetitionChallenge() != null
                    && s.getCompetitionChallenge().getChallenge() != null
                    && org.springframework.util.StringUtils.hasText(s.getCompetitionChallenge().getChallenge().getImageName());
            String cat = s.getCompetitionChallenge() != null
                    && s.getCompetitionChallenge().getChallenge() != null
                    && s.getCompetitionChallenge().getChallenge().getCategory() != null
                    ? s.getCompetitionChallenge().getChallenge().getCategory().getName()
                    : "Misc";
            String cName = s.getCompetitionChallenge() != null
                    && s.getCompetitionChallenge().getChallenge() != null
                    ? s.getCompetitionChallenge().getChallenge().getName()
                    : "";
            result.add(CtfTeamSolveRecordVO.builder()
                    .id(s.getId())
                    .challengeId(s.getCompetitionChallenge() != null ? s.getCompetitionChallenge().getId() : null)
                    .challengeName(cName)
                    .category(cat)
                    .submissionMode(isContainer ? "动态容器 Flag" : "静态 Flag")
                    .userId(s.getUser() != null ? s.getUser().getId() : null)
                    .username(s.getUser() != null ? s.getUser().getUsername() : null)
                    .points(s.getPointsAwarded())
                    .isFirstBlood(Boolean.TRUE.equals(s.getIsFirstBlood()))
                    .solvedAt(s.getSolvedAt())
                    .build());
        }
        return result;
    }

}
