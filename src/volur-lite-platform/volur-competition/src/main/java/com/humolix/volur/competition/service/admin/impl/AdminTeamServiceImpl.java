package com.humolix.volur.competition.service.admin.impl;

import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.admin.AdminTeamService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.alibaba.fastjson2.JSON;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTeamServiceImpl implements AdminTeamService {

    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResponse<AdminTeamListVO> listTeams(Long competitionId, Integer page, Integer size, TeamStatus status, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size, CompetitionTeam.class, null, null, "createdAt", org.springframework.data.domain.Sort.Direction.DESC);

        Specification<CompetitionTeam> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (competitionId != null) {
                predicates.add(cb.equal(root.get("competition").get("id"), competitionId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("teamName"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(teamRepository.findAll(spec, pageable).map(team -> {
            AdminTeamListVO vo = competitionMapper.toAdminTeamListVO(team);
            long count = memberRepository.countByTeamIdAndStatus(team.getId(), MemberStatus.INVITED_ACCEPTED);
            vo.setMemberCount((int) count);
            return vo;
        }));
    }

    @Override
    public AdminTeamDetailVO getTeam(Long teamId) {
        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        List<CompetitionMember> members = memberRepository.findByTeamId(teamId);

        return AdminTeamDetailVO.builder()
                .id(team.getId())
                .competitionId(team.getCompetition().getId())
                .competitionTitle(team.getCompetition().getTitle())
                .teamName(team.getTeamName())
                .avatarUrl(team.getAvatarUrl())
                .captainId(team.getCaptain() != null ? team.getCaptain().getId() : null)
                .captainUsername(team.getCaptain() != null ? team.getCaptain().getUsername() : null)
                .status(team.getStatus())
                .auditReason(team.getAuditReason())
                .members(competitionMapper.toMemberVOList(members))
                .createdAt(team.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditTeam(Long teamId, TeamAuditRequestDTO request) {
        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        if (request.getStatus() == TeamStatus.AUDIT_PASSED) {
            List<CompetitionMember> members = memberRepository.findByTeamId(teamId);
            int acceptedCount = 0;

            for (CompetitionMember m : members) {
                if (m.getStatus() == MemberStatus.INVITED) {
                    // 核心要求：若在通过队伍审核的时候依旧有队员未同意队伍邀请，则视为不参与比赛
                    m.setStatus(MemberStatus.NOT_PARTICIPATE);
                    memberRepository.save(m);
                    log.info("[Admin-Team] 队伍 {} 审核通过，成员 {} 仍未同意邀请，已自动置为 NOT_PARTICIPATE",
                            team.getTeamName(), m.getUser().getUsername());
                } else if (m.getStatus() == MemberStatus.INVITED_ACCEPTED) {
                    acceptedCount++;
                }
            }

            CompetitionConfig config = configRepository.findByCompetitionId(team.getCompetition().getId()).orElse(null);
            int minMembers = config != null && config.getMinTeamMembers() != null ? config.getMinTeamMembers() : 1;
            if (acceptedCount < minMembers) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "队伍中已确认参赛的成员人数不足 (" + acceptedCount + "/" + minMembers + "人)，无法通过审核");
            }

            team.setStatus(TeamStatus.AUDIT_PASSED);
            team.setAuditReason(null);
            teamRepository.save(team);

            // 向全队成员（队长、队员、领队）推送审核通过站内信
            Map<String, Object> passActionContext = new HashMap<>();
            passActionContext.put("competitionId", team.getCompetition().getId());
            if (team.getCompetition().getSlug() != null) {
                passActionContext.put("slug", team.getCompetition().getSlug());
            }
            passActionContext.put("teamId", team.getId());
            passActionContext.put("teamName", team.getTeamName());
            String passActionData = JSON.toJSONString(passActionContext);

            List<CompetitionMember> allMembers = memberRepository.findByTeamId(teamId);
            for (CompetitionMember m : allMembers) {
                if (m.getUser() != null) {
                    eventPublisher.publishEvent(SendNotificationEvent.builder()
                            .source(this)
                            .senderId(null)
                            .receiverId(m.getUser().getId())
                            .type(NotificationType.COMPETITION_AUDIT)
                            .title("【队伍审核通过】战队「" + team.getTeamName() + "」已通过审核")
                            .content(String.format("恭喜！您所在的战队「%s」已成功通过竞赛「%s」的参赛资格审核。",
                                    team.getTeamName(), team.getCompetition().getTitle()))
                            .actionStatus(NotificationActionStatus.NONE)
                            .actionData(passActionData)
                            .build());
                }
            }

        } else if (request.getStatus() == TeamStatus.AUDIT_REJECTED) {
            team.setStatus(TeamStatus.AUDIT_REJECTED);
            team.setAuditReason(request.getAuditReason());
            teamRepository.save(team);

            // 向全队成员（队长、队员、领队）推送驳回站内信
            Map<String, Object> rejectActionContext = new HashMap<>();
            rejectActionContext.put("competitionId", team.getCompetition().getId());
            if (team.getCompetition().getSlug() != null) {
                rejectActionContext.put("slug", team.getCompetition().getSlug());
            }
            rejectActionContext.put("teamId", team.getId());
            rejectActionContext.put("teamName", team.getTeamName());
            String rejectActionData = JSON.toJSONString(rejectActionContext);

            List<CompetitionMember> allMembers = memberRepository.findByTeamId(teamId);
            for (CompetitionMember m : allMembers) {
                if (m.getUser() != null) {
                    eventPublisher.publishEvent(SendNotificationEvent.builder()
                            .source(this)
                            .senderId(null)
                            .receiverId(m.getUser().getId())
                            .type(NotificationType.COMPETITION_AUDIT)
                            .title("【队伍审核未通过】战队「" + team.getTeamName() + "」审核被驳回")
                            .content(String.format("很遗憾，您所在的战队「%s」未能通过竞赛「%s」的审核。审核意见: %s",
                                    team.getTeamName(), team.getCompetition().getTitle(),
                                    StringUtils.hasText(request.getAuditReason()) ? request.getAuditReason() : "无"))
                            .actionStatus(NotificationActionStatus.NONE)
                            .actionData(rejectActionData)
                            .build());
                }
            }
        } else {
            team.setStatus(request.getStatus());
            team.setAuditReason(request.getAuditReason());
            teamRepository.save(team);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeam(Long teamId) {
        memberRepository.deleteByTeamId(teamId);
        teamRepository.deleteById(teamId);
    }

}
