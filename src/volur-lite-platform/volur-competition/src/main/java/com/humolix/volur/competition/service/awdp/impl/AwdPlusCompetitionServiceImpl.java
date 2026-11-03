package com.humolix.volur.competition.service.awdp.impl;

import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.awdp.AwdPlusPatchUploadRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.awdp.AwdPlusCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.awdp.AwdPlusCompetitionPatch;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.awdp.AwdPlusTargetStatusVO;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.awdp.AwdPlusCompetitionChallengeRepository;
import com.humolix.volur.competition.repository.awdp.AwdPlusCompetitionPatchRepository;
import com.humolix.volur.competition.service.awdp.AwdPlusCompetitionService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.AWD_PLUS})
@Transactional(readOnly = true)
public class AwdPlusCompetitionServiceImpl implements AwdPlusCompetitionService {

    private final CompetitionMemberRepository memberRepository;
    private final AwdPlusCompetitionChallengeRepository challengeRepository;
    private final AwdPlusCompetitionPatchRepository patchRepository;
    private final UserRepository userRepository;

    @Override
    public List<AwdPlusTargetStatusVO> listTargets(Long competitionId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
        List<AwdPlusCompetitionChallenge> challenges = challengeRepository.findByCompetitionIdWithChallenge(competitionId);
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);

        Map<Long, String> patchStatusMap = Map.of();
        if (team != null) {
            List<AwdPlusCompetitionPatch> patches = patchRepository.findByCompetitionIdAndTeamId(competitionId, team.getId());
            patchStatusMap = patches.stream()
                    .collect(Collectors.toMap(p -> p.getChallenge().getId(), AwdPlusCompetitionPatch::getVerifyStatus, (k1, k2) -> k2));
        }

        List<AwdPlusTargetStatusVO> list = new ArrayList<>();
        for (AwdPlusCompetitionChallenge c : challenges) {
            AwdPlusChallenge raw = c.getChallenge();
            String patchStatus = patchStatusMap.getOrDefault(c.getId(), "UNPATCHED");
            list.add(AwdPlusTargetStatusVO.builder()
                    .challengeId(c.getId())
                    .challengeName(raw != null ? raw.getName() : "AWD+ Challenge")
                    .category(raw != null ? raw.getCategory() : "MISC")
                    .initialPoints(c.getInitialPoints())
                    .currentPoints(c.getCurrentPoints())
                    .patchStatus(patchStatus)
                    .sourceCodeUrl(raw != null ? raw.getSourceCodeUrl() : null)
                    .build());
        }
        return list;
    }

    @Override
    @Transactional
    public void uploadPatch(Long competitionId, Long userId, AwdPlusPatchUploadRequestDTO request) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        AwdPlusCompetitionChallenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD+ 题目不存在"));

        Competition comp = challenge.getCompetition();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        AwdPlusCompetitionPatch patch = AwdPlusCompetitionPatch.builder()
                .competition(comp)
                .challenge(challenge)
                .team(team)
                .user(user)
                .patchFileUrl(request.getPatchFileUrl().trim())
                .verifyStatus("PENDING")
                .build();

        patchRepository.save(patch);
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
