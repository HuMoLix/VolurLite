package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.annotation.RequiredCompetitionRunning;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionStageConfig;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.stage.awd.AwdCompetitionLog;
import com.humolix.volur.competition.domain.entity.stage.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.entity.stage.theory.TheoryCompetitionAnswer;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.scoreboard.*;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionStageConfigRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.stage.awd.AwdCompetitionLogRepository;
import com.humolix.volur.competition.repository.stage.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.stage.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.service.CompetitionScoreboardService;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@RequiredCompetitionRunning
public class CompetitionScoreboardServiceImpl implements CompetitionScoreboardService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionStageConfigRepository stageConfigRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CtfCompetitionSolvedRepository ctfSolvedRepository;
    private final TheoryCompetitionAnswerRepository theoryAnswerRepository;
    private final AwdCompetitionLogRepository awdLogRepository;

    @Override
    public CtfStageScoreboardVO getCtfStageScoreboard(Long competitionId) {
        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        boolean isFrozen = config != null && Boolean.TRUE.equals(config.getIsScoreboardFrozen());

        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED);
        List<CtfCompetitionSolved> allSolves = ctfSolvedRepository.findAllSolvesWithDetails(competitionId);

        Map<Long, Integer> teamScoreMap = new HashMap<>();
        Map<Long, Integer> teamSolvesMap = new HashMap<>();
        Map<Long, Instant> teamLastSolveMap = new HashMap<>();

        for (CtfCompetitionSolved solve : allSolves) {
            Long teamId = solve.getTeam().getId();
            teamScoreMap.put(teamId, teamScoreMap.getOrDefault(teamId, 0) + solve.getPointsAwarded());
            teamSolvesMap.put(teamId, teamSolvesMap.getOrDefault(teamId, 0) + 1);

            Instant currentLast = teamLastSolveMap.get(teamId);
            if (currentLast == null || solve.getSolvedAt().isAfter(currentLast)) {
                teamLastSolveMap.put(teamId, solve.getSolvedAt());
            }
        }

        List<CtfTeamRankVO> rankList = new ArrayList<>();
        for (CompetitionTeam team : teams) {
            rankList.add(CtfTeamRankVO.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .avatarUrl(team.getAvatarUrl())
                    .score(teamScoreMap.getOrDefault(team.getId(), 0))
                    .solvesCount(teamSolvesMap.getOrDefault(team.getId(), 0))
                    .lastSolveTime(teamLastSolveMap.get(team.getId()))
                    .build());
        }

        rankList.sort((a, b) -> {
            int cmp = Integer.compare(b.getScore(), a.getScore());
            if (cmp != 0) return cmp;
            if (a.getLastSolveTime() == null && b.getLastSolveTime() == null) return 0;
            if (a.getLastSolveTime() == null) return 1;
            if (b.getLastSolveTime() == null) return -1;
            return a.getLastSolveTime().compareTo(b.getLastSolveTime());
        });

        for (int i = 0; i < rankList.size(); i++) {
            rankList.get(i).setRank(i + 1);
        }

        return CtfStageScoreboardVO.builder()
                .competitionId(competitionId)
                .isFrozen(isFrozen)
                .rankings(rankList)
                .build();
    }

    @Override
    public CompositeScoreboardVO getCompositeScoreboard(Long competitionId) {
        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        boolean isFrozen = config != null && Boolean.TRUE.equals(config.getIsScoreboardFrozen());

        List<CompetitionStageConfig> stageConfigs = stageConfigRepository.findByCompetitionIdOrderByStageOrderAsc(competitionId);
        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED);

        // Calculate raw scores per stage for each team: teamId -> (stage -> rawScore)
        Map<Long, Map<CompetitionStage, Double>> teamStageRawScore = new HashMap<>();
        for (CompetitionTeam t : teams) {
            teamStageRawScore.put(t.getId(), new HashMap<>());
        }

        // 1. CTF Solves
        List<CtfCompetitionSolved> allSolves = ctfSolvedRepository.findAllSolvesWithDetails(competitionId);
        for (CtfCompetitionSolved solve : allSolves) {
            Long tid = solve.getTeam().getId();
            if (teamStageRawScore.containsKey(tid)) {
                Map<CompetitionStage, Double> map = teamStageRawScore.get(tid);
                map.put(CompetitionStage.CTF, map.getOrDefault(CompetitionStage.CTF, 0.0) + solve.getPointsAwarded());
            }
        }

        // 2. Theory Answers
        for (CompetitionTeam t : teams) {
            List<TheoryCompetitionAnswer> answers = theoryAnswerRepository.findByCompetitionIdAndTeamId(competitionId, t.getId());
            double theoryScore = answers.stream().mapToDouble(TheoryCompetitionAnswer::getScoreObtained).sum();
            teamStageRawScore.get(t.getId()).put(CompetitionStage.THEORY, theoryScore);
        }

        // 3. AWD Logs
        for (CompetitionTeam t : teams) {
            List<AwdCompetitionLog> attacks = awdLogRepository.findByCompetitionIdAndAttackerTeamId(competitionId, t.getId());
            double attackPts = attacks.stream().mapToDouble(AwdCompetitionLog::getPointsDelta).sum();
            teamStageRawScore.get(t.getId()).put(CompetitionStage.AWD, attackPts);
        }

        List<CompositeTeamScoreVO> compositeList = new ArrayList<>();
        for (CompetitionTeam team : teams) {
            Map<CompetitionStage, Double> rawMap = teamStageRawScore.get(team.getId());
            List<StageScoreDetailVO> stageDetails = new ArrayList<>();
            double totalWeighted = 0.0;

            for (CompetitionStageConfig sc : stageConfigs) {
                double raw = rawMap.getOrDefault(sc.getStage(), 0.0);
                double weight = sc.getWeightPercent() != null ? sc.getWeightPercent() : 100.0;
                double weighted = raw * (weight / 100.0);
                totalWeighted += weighted;

                stageDetails.add(StageScoreDetailVO.builder()
                        .stage(sc.getStage())
                        .stageName(sc.getStageName())
                        .rawScore(Math.round(raw * 100.0) / 100.0)
                        .weightPercent(weight)
                        .weightedScore(Math.round(weighted * 100.0) / 100.0)
                        .build());
            }

            compositeList.add(CompositeTeamScoreVO.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .avatarUrl(team.getAvatarUrl())
                    .totalFinalScore(Math.round(totalWeighted * 100.0) / 100.0)
                    .stageScores(stageDetails)
                    .build());
        }

        compositeList.sort((a, b) -> Double.compare(b.getTotalFinalScore(), a.getTotalFinalScore()));
        for (int i = 0; i < compositeList.size(); i++) {
            compositeList.get(i).setRank(i + 1);
        }

        return CompositeScoreboardVO.builder()
                .competitionId(competitionId)
                .competitionTitle(comp.getTitle())
                .isFrozen(isFrozen)
                .rankings(compositeList)
                .build();
    }

}
