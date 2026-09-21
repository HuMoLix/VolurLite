package com.humolix.volur.competition.service.admin.impl;

import com.humolix.volur.competition.domain.dto.AdminTeamUpdateDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamAuditDTO;
import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionChatMessageRepository;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionTeamInvitationRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.awdp.AwdPlusCompetitionPatchRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.service.admin.AdminTeamService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.alibaba.fastjson2.JSON;
import com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.Gender;
import com.humolix.volur.user.repository.UserRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTeamServiceImpl implements AdminTeamService {

    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionChatMessageRepository chatMessageRepository;
    private final CompetitionTeamInvitationRepository teamInvitationRepository;
    private final CtfCompetitionSubmissionRepository ctfSubmissionRepository;
    private final TheoryCompetitionAnswerRepository theoryAnswerRepository;
    private final AwdPlusCompetitionPatchRepository awdPlusPatchRepository;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

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
                .description(team.getDescription())
                .organization(team.getOrganization())
                .organizationType(team.getOrganizationType() != null ? team.getOrganizationType() : com.humolix.volur.competition.domain.enums.OrganizationType.SCHOOL)
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
                            team.getTeamName(), m.getUser() != null ? m.getUser().getUsername() : m.getRealName());
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
        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        // 1. 安全前置校验：若队伍已存在赛题提交/作答记录，禁止物理删除以保证比赛成绩与日志数据完整性
        if (ctfSubmissionRepository.existsByTeamId(teamId)
                || theoryAnswerRepository.existsByTeamId(teamId)
                || awdPlusPatchRepository.existsByTeamId(teamId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该战队已有赛题作答或提交记录，无法直接删除！如需取消参赛资格，请在审核操作中将其驳回");
        }

        // 2. 级联清理队伍附属数据（邀请、聊天工单、队员关系）
        teamInvitationRepository.deleteByTeamId(teamId);
        chatMessageRepository.deleteByTeamId(teamId);
        memberRepository.deleteByTeamId(teamId);

        // 3. 删除队伍主体
        teamRepository.delete(team);
        log.info("[Admin-Team] 管理员删除了战队 ID: {}, 战队名称: {}", teamId, team.getTeamName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeam(Long teamId, AdminTeamUpdateDTO request) {
        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        String newTeamName = request.getTeamName().trim();
        // 校验同比赛下是否与其他队伍重名（排除自身）
        if (teamRepository.existsByCompetitionIdAndTeamNameAndIdNot(team.getCompetition().getId(), newTeamName, teamId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛中已存在同名战队「" + newTeamName + "」，无法修改为该名称");
        }

        team.setTeamName(newTeamName);
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription().trim());
        }
        if (request.getOrganization() != null) {
            team.setOrganization(request.getOrganization().trim());
        }
        if (request.getOrganizationType() != null) {
            team.setOrganizationType(request.getOrganizationType());
        }

        // 处理参赛成员更新 (全信息更新 & 支持修改队长)
        if (request.getMembers() != null) {
            if (request.getMembers().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛成员名册不能为空，至少需保留1名队长");
            }

            List<CompetitionMember> allMembers = memberRepository.findByTeamId(teamId);
            Map<Long, CompetitionMember> regularMemberMap = allMembers.stream()
                    .filter(m -> m.getRole() != MemberRole.LEADER)
                    .collect(Collectors.toMap(CompetitionMember::getId, m -> m));

            // 识别指定的队长：若有多个只保留第一个作为队长；若未指定则默认第1个为队长
            int captainIndex = -1;
            for (int i = 0; i < request.getMembers().size(); i++) {
                if (request.getMembers().get(i).getRole() == MemberRole.CAPTAIN) {
                    captainIndex = i;
                    break;
                }
            }
            if (captainIndex == -1) {
                captainIndex = 0;
            }

            Set<Long> processedMemberIds = new HashSet<>();
            User newCaptainUser = null;

            for (int i = 0; i < request.getMembers().size(); i++) {
                AdminTeamMemberDTO memberDTO = request.getMembers().get(i);
                boolean isCaptain = (i == captainIndex);

                if (memberDTO.getId() != null && regularMemberMap.containsKey(memberDTO.getId())) {
                    CompetitionMember existing = regularMemberMap.get(memberDTO.getId());
                    processedMemberIds.add(existing.getId());

                    if (StringUtils.hasText(memberDTO.getRealName())) existing.setRealName(memberDTO.getRealName().trim());
                    if (memberDTO.getGender() != null) existing.setGender(memberDTO.getGender());
                    existing.setPhone(memberDTO.getPhone() != null ? memberDTO.getPhone().trim() : null);
                    existing.setIdCard(memberDTO.getIdCard() != null ? memberDTO.getIdCard().trim().toUpperCase() : null);
                    existing.setEmployeeNumber(memberDTO.getEmployeeNumber() != null ? memberDTO.getEmployeeNumber().trim() : null);
                    existing.setDepartment(memberDTO.getDepartment() != null ? memberDTO.getDepartment().trim() : null);
                    existing.setPosition(memberDTO.getPosition() != null ? memberDTO.getPosition().trim() : null);

                    if (StringUtils.hasText(memberDTO.getEmail())) {
                        String email = memberDTO.getEmail().trim().toLowerCase();
                        existing.setEmail(email);
                        userRepository.findByEmail(email).ifPresent(existing::setUser);
                    }

                    existing.setRole(isCaptain ? MemberRole.CAPTAIN : MemberRole.MEMBER);
                    memberRepository.save(existing);

                    if (isCaptain && existing.getUser() != null) {
                        newCaptainUser = existing.getUser();
                    }
                } else {
                    User memberUser = null;
                    if (StringUtils.hasText(memberDTO.getEmail())) {
                        memberUser = userRepository.findByEmail(memberDTO.getEmail().trim().toLowerCase()).orElse(null);
                    }
                    CompetitionMember newMember = CompetitionMember.builder()
                            .competition(team.getCompetition())
                            .team(team)
                            .user(memberUser)
                            .role(isCaptain ? MemberRole.CAPTAIN : MemberRole.MEMBER)
                            .status(MemberStatus.INVITED_ACCEPTED)
                            .realName(memberDTO.getRealName() != null ? memberDTO.getRealName().trim() : null)
                            .gender(memberDTO.getGender() != null ? memberDTO.getGender() : Gender.MALE)
                            .phone(memberDTO.getPhone() != null ? memberDTO.getPhone().trim() : null)
                            .email(memberDTO.getEmail() != null ? memberDTO.getEmail().trim().toLowerCase() : null)
                            .idCard(memberDTO.getIdCard() != null ? memberDTO.getIdCard().trim().toUpperCase() : null)
                            .employeeNumber(memberDTO.getEmployeeNumber() != null ? memberDTO.getEmployeeNumber().trim() : null)
                            .department(memberDTO.getDepartment() != null ? memberDTO.getDepartment().trim() : null)
                            .position(memberDTO.getPosition() != null ? memberDTO.getPosition().trim() : null)
                            .build();
                    CompetitionMember savedNew = memberRepository.save(newMember);
                    if (isCaptain && memberUser != null) {
                        newCaptainUser = memberUser;
                    }
                }
            }

            // 删除被移除的队员
            for (CompetitionMember m : regularMemberMap.values()) {
                if (!processedMemberIds.contains(m.getId())) {
                    memberRepository.delete(m);
                }
            }

            if (newCaptainUser != null) {
                team.setCaptain(newCaptainUser);
            }
        }

        // 处理指导老师 / 领队更新
        if (request.getLeaders() != null) {
            List<CompetitionMember> allMembers = memberRepository.findByTeamId(teamId);
            Map<Long, CompetitionMember> leaderMap = allMembers.stream()
                    .filter(m -> m.getRole() == MemberRole.LEADER)
                    .collect(Collectors.toMap(CompetitionMember::getId, m -> m));

            Set<Long> processedLeaderIds = new HashSet<>();
            for (AdminTeamMemberDTO leaderDTO : request.getLeaders()) {
                if (leaderDTO.getId() != null && leaderMap.containsKey(leaderDTO.getId())) {
                    CompetitionMember leader = leaderMap.get(leaderDTO.getId());
                    processedLeaderIds.add(leader.getId());

                    if (StringUtils.hasText(leaderDTO.getRealName())) leader.setRealName(leaderDTO.getRealName().trim());
                    if (leaderDTO.getGender() != null) leader.setGender(leaderDTO.getGender());
                    leader.setPhone(leaderDTO.getPhone() != null ? leaderDTO.getPhone().trim() : null);
                    leader.setEmail(leaderDTO.getEmail() != null ? leaderDTO.getEmail().trim().toLowerCase() : null);
                    leader.setIdCard(leaderDTO.getIdCard() != null ? leaderDTO.getIdCard().trim().toUpperCase() : null);
                    leader.setEmployeeNumber(leaderDTO.getEmployeeNumber() != null ? leaderDTO.getEmployeeNumber().trim() : null);
                    leader.setDepartment(leaderDTO.getDepartment() != null ? leaderDTO.getDepartment().trim() : null);
                    leader.setPosition(leaderDTO.getPosition() != null ? leaderDTO.getPosition().trim() : null);
                    memberRepository.save(leader);
                } else {
                    User leaderUser = null;
                    if (StringUtils.hasText(leaderDTO.getEmail())) {
                        leaderUser = userRepository.findByEmail(leaderDTO.getEmail().trim().toLowerCase()).orElse(null);
                    }
                    CompetitionMember newLeader = CompetitionMember.builder()
                            .competition(team.getCompetition())
                            .team(team)
                            .user(leaderUser)
                            .role(MemberRole.LEADER)
                            .status(MemberStatus.INVITED_ACCEPTED)
                            .realName(leaderDTO.getRealName() != null ? leaderDTO.getRealName().trim() : null)
                            .gender(leaderDTO.getGender() != null ? leaderDTO.getGender() : Gender.MALE)
                            .phone(leaderDTO.getPhone() != null ? leaderDTO.getPhone().trim() : null)
                            .email(leaderDTO.getEmail() != null ? leaderDTO.getEmail().trim().toLowerCase() : null)
                            .idCard(leaderDTO.getIdCard() != null ? leaderDTO.getIdCard().trim().toUpperCase() : null)
                            .employeeNumber(leaderDTO.getEmployeeNumber() != null ? leaderDTO.getEmployeeNumber().trim() : null)
                            .department(leaderDTO.getDepartment() != null ? leaderDTO.getDepartment().trim() : null)
                            .position(leaderDTO.getPosition() != null ? leaderDTO.getPosition().trim() : null)
                            .build();
                    memberRepository.save(newLeader);
                }
            }

            for (CompetitionMember l : leaderMap.values()) {
                if (!processedLeaderIds.contains(l.getId())) {
                    memberRepository.delete(l);
                }
            }
        }

        teamRepository.save(team);
        log.info("[Admin-Team] 管理员更新了战队 ID: {} 的基本信息, 新名称: {}", teamId, newTeamName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAuditTeams(Long competitionId, BatchTeamAuditDTO request) {
        if (request.getTeamIds() == null || request.getTeamIds().isEmpty()) {
            return;
        }
        TeamAuditRequestDTO auditRequest = new TeamAuditRequestDTO();
        auditRequest.setStatus(request.getStatus());
        auditRequest.setAuditReason(request.getAuditReason());

        for (Long teamId : request.getTeamIds()) {
            auditTeam(teamId, auditRequest);
        }
        log.info("[Admin-Team] 管理员批量审核了竞赛 {} 下的 {} 支战队，状态: {}",
                competitionId, request.getTeamIds().size(), request.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteTeams(Long competitionId, List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return;
        }
        for (Long teamId : teamIds) {
            deleteTeam(teamId);
        }
        log.info("[Admin-Team] 管理员批量删除了竞赛 {} 下的 {} 支战队", competitionId, teamIds.size());
    }

}
