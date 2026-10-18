package com.humolix.volur.competition.service.awd.impl;

import com.humolix.volur.challenge.domain.entity.awd.AwdChallenge;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.awd.AwdFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionLog;
import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionRound;
import com.humolix.volur.competition.domain.enums.AwdRoundStatus;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.awd.AwdCurrentRoundVO;
import com.humolix.volur.competition.domain.vo.awd.AwdTargetStatusVO;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.awd.AwdCompetitionChallengeRepository;
import com.humolix.volur.competition.repository.awd.AwdCompetitionLogRepository;
import com.humolix.volur.competition.repository.awd.AwdCompetitionRoundRepository;
import com.humolix.volur.competition.service.awd.AwdCompetitionService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.AWD})
@Transactional(readOnly = true)
public class AwdCompetitionServiceImpl implements AwdCompetitionService {

    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final AwdCompetitionChallengeRepository challengeRepository;
    private final AwdCompetitionRoundRepository roundRepository;
    private final AwdCompetitionLogRepository logRepository;
    private final UserRepository userRepository;

    @Override
    public AwdCurrentRoundVO getCurrentRound(Long competitionId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
        Optional<AwdCompetitionRound> runningRoundOpt = roundRepository.findFirstByCompetitionIdAndStatus(competitionId, AwdRoundStatus.RUNNING);
        int roundNumber = runningRoundOpt.map(AwdCompetitionRound::getRoundNumber).orElse(1);
        Instant startTime = runningRoundOpt.map(AwdCompetitionRound::getStartTime).orElse(Instant.now());
        Instant endTime = runningRoundOpt.map(AwdCompetitionRound::getEndTime).orElse(Instant.now().plus(Duration.ofMinutes(10)));
        long remaining = Math.max(0, Duration.between(Instant.now(), endTime).getSeconds());

        List<AwdCompetitionChallenge> challenges = challengeRepository.findByCompetitionIdWithChallenge(competitionId);
        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED);

        List<AwdTargetStatusVO> targets = new ArrayList<>();
        for (AwdCompetitionChallenge c : challenges) {
            AwdChallenge raw = c.getChallenge();
            for (CompetitionTeam t : teams) {
                targets.add(AwdTargetStatusVO.builder()
                        .challengeId(c.getId())
                        .challengeName(raw != null ? raw.getName() : "AWD Challenge")
                        .teamId(t.getId())
                        .teamName(t.getTeamName())
                        .host("10." + competitionId + "." + t.getId() + ".1")
                        .port(raw != null ? raw.getContainerPort() : 80)
                        .status("NORMAL")
                        .currentPoints(c.getCurrentPoints())
                        .build());
            }
        }

        return AwdCurrentRoundVO.builder()
                .roundNumber(roundNumber)
                .startTime(startTime)
                .endTime(endTime)
                .remainingSeconds((int) remaining)
                .targets(targets)
                .build();
    }

    @Override
    @Transactional
    public boolean submitFlag(Long competitionId, Long userId, AwdFlagSubmitRequestDTO request) {
        CompetitionTeam attackerTeam = getMyApprovedTeam(competitionId, userId);
        if (attackerTeam == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        if (attackerTeam.getId().equals(request.getVictimTeamId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能提交自己队伍的 Flag");
        }

        AwdCompetitionChallenge compChal = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD 题目不存在"));

        Competition comp = compChal.getCompetition();

        CompetitionTeam victimTeam = teamRepository.findById(request.getVictimTeamId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标队伍不存在"));

        Optional<AwdCompetitionRound> runningRoundOpt = roundRepository.findFirstByCompetitionIdAndStatus(competitionId, AwdRoundStatus.RUNNING);
        int roundNumber = runningRoundOpt.map(AwdCompetitionRound::getRoundNumber).orElse(1);

        // Record attack log
        AwdCompetitionLog attackLog = AwdCompetitionLog.builder()
                .competition(comp)
                .roundNumber(roundNumber)
                .attackerTeam(attackerTeam)
                .victimTeam(victimTeam)
                .challenge(compChal)
                .pointsDelta(100)
                .logType("ATTACK")
                .message("队伍 " + attackerTeam.getTeamName() + " 攻破了队伍 " + victimTeam.getTeamName() + " 的靶机 " + compChal.getId())
                .build();

        logRepository.save(attackLog);
        return true;
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

    private void validateUserParticipationOrAdmin(Long competitionId, Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team != null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        boolean isAdmin = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        if (!isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }
    }

}
