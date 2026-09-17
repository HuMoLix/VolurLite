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
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.humolix.volur.competition.domain.enums.CompetitionType;

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

    

    @Override
    public List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId) {
        List<CtfCompetitionChallenge> challenges = challengeRepository.findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc(competitionId);

        List<CtfCompetitionChallengeVO> voList = competitionMapper.toCtfChallengeVOList(challenges);

        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        Set<Long> solvedChallengeIds = Collections.emptySet();
        if (myTeam != null) {
            solvedChallengeIds = solvedRepository.findByCompetitionIdAndTeamId(competitionId, myTeam.getId())
                    .stream()
                    .map(s -> s.getCompetitionChallenge().getId())
                    .collect(Collectors.toSet());
        }

        for (CtfCompetitionChallengeVO vo : voList) {
            vo.setIsSolvedByMyTeam(solvedChallengeIds.contains(vo.getId()));
        }

        return voList;
    }

    @Override
    public CtfCompetitionChallengeDetailVO getChallengeDetail(Long competitionId, Long competitionChallengeId, Long userId) {
        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        if (!compChallenge.getCompetition().getId().equals(competitionId) || !Boolean.TRUE.equals(compChallenge.getIsVisible())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在或尚未公开");
        }

        CtfCompetitionChallengeDetailVO vo = competitionMapper.toCtfChallengeDetailVO(compChallenge);
        CtfChallenge rawChallenge = compChallenge.getChallenge();
        vo.setIsContainer(rawChallenge != null && rawChallenge.isDynamicContainer());

        CompetitionTeam myTeam = getMyApprovedTeam(competitionId, userId);
        boolean isSolved = myTeam != null && solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(
                competitionId, competitionChallengeId, myTeam.getId()
        );
        vo.setIsSolvedByMyTeam(isSolved);

        return vo;
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

        return true;
    }

    @Override
    public ChallengeInstanceResponseVO startInstance(Long competitionId, Long competitionChallengeId, Long userId) {
        CompetitionTeam team = getMyApprovedTeamOrThrow(competitionId, userId);

        CtfCompetitionChallenge compChallenge = challengeRepository.findByIdWithChallenge(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));

        Long stageId = 0L;
        String teamToken = null;

        return containerInstanceService.startInstance(
                compChallenge.getChallenge().getId(),
                userId,
                stageId,
                team.getId(),
                teamToken
        );
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
