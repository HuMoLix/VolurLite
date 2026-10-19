package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.awd.AwdCompetitionLog;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionAnswer;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.scoreboard.*;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.awd.AwdCompetitionLogRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.service.CompetitionScoreboardService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING})
public class CompetitionScoreboardServiceImpl implements CompetitionScoreboardService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CtfCompetitionSolvedRepository ctfSolvedRepository;
    private final TheoryCompetitionAnswerRepository theoryAnswerRepository;
    private final AwdCompetitionLogRepository awdLogRepository;
    private final VolurCacheTemplate cacheTemplate;

    @Override
    public CtfScoreboardVO getCtfScoreboard(Long competitionId) {
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "scoreboard:ctf:" + competitionId,
                () -> computeCtfScoreboard(competitionId)
        );
    }

    private CtfScoreboardVO computeCtfScoreboard(Long competitionId) {
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

        return CtfScoreboardVO.builder()
                .competitionId(competitionId)
                .isFrozen(isFrozen)
                .rankings(rankList)
                .build();
    }

    @Override
    public CompositeScoreboardVO getCompositeScoreboard(Long competitionId) {
        return cacheTemplate.getOrLoad(
                CacheNames.COMPETITION_STATUS,
                "scoreboard:composite:" + competitionId,
                () -> computeCompositeScoreboard(competitionId)
        );
    }

    private CompositeScoreboardVO computeCompositeScoreboard(Long competitionId) {
        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        boolean isFrozen = config != null && Boolean.TRUE.equals(config.getIsScoreboardFrozen());

        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatusIn(competitionId, List.of(TeamStatus.AUDIT_PASSED, TeamStatus.ENDED));
        CompetitionType type = comp.getType() != null ? comp.getType() : CompetitionType.CTF;

        Map<Long, Double> teamScores = new HashMap<>();
        for (CompetitionTeam t : teams) {
            teamScores.put(t.getId(), 0.0);
        }

        switch (type) {
            case CTF -> {
                List<CtfCompetitionSolved> allSolves = ctfSolvedRepository.findAllSolvesWithDetails(competitionId);
                for (CtfCompetitionSolved solve : allSolves) {
                    Long tid = solve.getTeam().getId();
                    if (teamScores.containsKey(tid)) {
                        teamScores.put(tid, teamScores.get(tid) + solve.getPointsAwarded());
                    }
                }
            }
            case THEORY -> {
                for (CompetitionTeam t : teams) {
                    List<TheoryCompetitionAnswer> answers = theoryAnswerRepository.findByCompetitionIdAndTeamId(competitionId, t.getId());
                    double theoryScore = answers.stream().mapToDouble(TheoryCompetitionAnswer::getScoreObtained).sum();
                    teamScores.put(t.getId(), theoryScore);
                }
            }
            case AWD, AWD_PLUS -> {
                for (CompetitionTeam t : teams) {
                    List<AwdCompetitionLog> attacks = awdLogRepository.findByCompetitionIdAndAttackerTeamId(competitionId, t.getId());
                    double attackPts = attacks.stream().mapToDouble(AwdCompetitionLog::getPointsDelta).sum();
                    teamScores.put(t.getId(), attackPts);
                }
            }
            case ISW -> {
                // ISW single-type scoring placeholder
            }
        }

        List<CompositeTeamScoreVO> compositeList = new ArrayList<>();
        for (CompetitionTeam team : teams) {
            double raw = teamScores.getOrDefault(team.getId(), 0.0);
            double rounded = Math.round(raw * 100.0) / 100.0;

            List<CompetitionScoreDetailVO> stageDetails = List.of(
                    CompetitionScoreDetailVO.builder()
                            .type(type)
                            .typeName(type.getDescription())
                            .rawScore(rounded)
                            .weightPercent(100.0)
                            .weightedScore(rounded)
                            .build()
            );

            compositeList.add(CompositeTeamScoreVO.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .avatarUrl(team.getAvatarUrl())
                    .totalFinalScore(rounded)
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
