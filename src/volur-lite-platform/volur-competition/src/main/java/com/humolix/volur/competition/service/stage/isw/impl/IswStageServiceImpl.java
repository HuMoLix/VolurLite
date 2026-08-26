package com.humolix.volur.competition.service.stage.isw.impl;

import com.humolix.volur.challenge.domain.entity.stage.isw.IswChallenge;
import com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning;
import com.humolix.volur.competition.domain.dto.stage.isw.IswFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.stage.isw.IswCompetitionChallenge;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.stage.isw.IswCompetitionTopologyVO;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.stage.isw.IswCompetitionChallengeRepository;
import com.humolix.volur.competition.service.stage.isw.IswStageService;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStageRunning(stage = CompetitionStage.ISW)
public class IswStageServiceImpl implements IswStageService {

    private final CompetitionMemberRepository memberRepository;
    private final IswCompetitionChallengeRepository challengeRepository;

    @Override
    public List<IswCompetitionTopologyVO> listScenarios(Long competitionId, Long userId) {
        List<IswCompetitionChallenge> challenges = challengeRepository.findByCompetitionIdWithChallenge(competitionId);
        List<IswCompetitionTopologyVO> list = new ArrayList<>();
        for (IswCompetitionChallenge c : challenges) {
            IswChallenge raw = c.getChallenge();
            list.add(IswCompetitionTopologyVO.builder()
                    .challengeId(c.getId())
                    .name(raw != null ? raw.getName() : "ISW Scenario")
                    .description(raw != null ? raw.getDescription() : "")
                    .category(raw != null ? raw.getCategory() : "PENETRATION")
                    .initialPoints(c.getInitialPoints())
                    .currentPoints(c.getCurrentPoints())
                    .topologyJson(raw != null ? raw.getTopologyJson() : "{}")
                    .unlockedStage(1)
                    .build());
        }
        return list;
    }

    @Override
    public boolean submitFlag(Long competitionId, Long userId, IswFlagSubmitRequestDTO request) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        IswCompetitionChallenge compChal = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ISW 题目不存在"));

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

}
