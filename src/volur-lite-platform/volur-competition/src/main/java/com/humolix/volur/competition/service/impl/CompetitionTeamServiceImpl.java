package com.humolix.volur.competition.service.impl;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamMemberSignupDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.entity.*;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionMemberVO;
import com.humolix.volur.competition.domain.vo.CompetitionTeamVO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.domain.vo.TeamInvitationVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.*;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.DesensitizationUtil;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.humolix.volur.notification.service.NotificationService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.Gender;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionTeamServiceImpl implements CompetitionTeamService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})
    public Long signupTeam(Long competitionId, Long captainUserId, TeamSignupRequestDTO request) {

        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        List<CompetitionMember> existingMembers = memberRepository.findAllByCompetitionIdAndUserId(competitionId, captainUserId);
        boolean alreadyInTeam = existingMembers.stream().anyMatch(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED);
        if (alreadyInTeam) {
            throw new BusinessException(HttpStatus.CONFLICT, "您已在该竞赛中创建或加入了队伍");
        }

        if (teamRepository.existsByCompetitionIdAndTeamName(competitionId, request.getTeamName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "队伍名称已存在");
        }

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        if (config != null && config.getMaxTeams() != null) {
            long currentTeams = teamRepository.countByCompetitionId(competitionId);
            if (currentTeams >= config.getMaxTeams()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "本场竞赛报名队伍数量已达上限");
            }
        }

        int maxLeaders = config != null && config.getMaxLeaders() != null ? config.getMaxLeaders() : 1;
        int maxMembers = config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4;
        int minMembers = config != null && config.getMinTeamMembers() != null ? config.getMinTeamMembers() : 1;

        List<TeamMemberSignupDTO> memberList = request.getMembers() != null ? request.getMembers() : Collections.emptyList();
        if (memberList.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛成员名册不能为空");
        }
        if (memberList.size() > maxMembers + maxLeaders) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "名册成员总行数超过上限 (" + (maxMembers + maxLeaders) + "行)");
        }

        // 2. 角色区分与数量校验
        List<TeamMemberSignupDTO> captainRows = new ArrayList<>();
        List<TeamMemberSignupDTO> leaderRows = new ArrayList<>();
        List<TeamMemberSignupDTO> normalMemberRows = new ArrayList<>();

        for (TeamMemberSignupDTO m : memberList) {
            if (m.getRole() == MemberRole.CAPTAIN) {
                captainRows.add(m);
            } else if (m.getRole() == MemberRole.LEADER) {
                leaderRows.add(m);
            } else if (m.getRole() == MemberRole.MEMBER) {
                normalMemberRows.add(m);
            } else {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "未知的成员角色类型");
            }
        }

        if (captainRows.size() != 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍必须且只能包含 1 名队长");
        }
        TeamMemberSignupDTO captainRow = captainRows.getFirst();

        if (leaderRows.size() > maxLeaders) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (最多 " + maxLeaders + " 人)");
        }

        int totalTeamMembers = 1 + normalMemberRows.size();
        if (totalTeamMembers > maxMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛队员人数超过上限 (" + maxMembers + "人，含队长)");
        }
        if (totalTeamMembers < minMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍总人数未达到最低要求 (" + minMembers + "人，含队长)");
        }

        User captain = userRepository.findById(captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (captain.getEmail() == null || !captain.getEmail().equalsIgnoreCase(captainRow.getEmail().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队长邮箱必须为当前登录账号绑定的邮箱 (" + captain.getEmail() + ")");
        }

        Set<String> seenEmails = new HashSet<>();
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenIdCards = new HashSet<>();
        Set<String> seenStudentNumbers = new HashSet<>();
        for (TeamMemberSignupDTO m : memberList) {
            String studentNumber = m.getStudentNumber() != null ? m.getStudentNumber().trim() : "";
            if (!StringUtils.hasText(studentNumber)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员学号不能为空");
            }
            if (!seenStudentNumbers.add(studentNumber)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员学号重复: " + studentNumber);
            }

            String phone = m.getPhone() != null ? m.getPhone().trim() : "";
            if (!StringUtils.hasText(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员手机号不能为空");
            }
            if (!seenPhones.add(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员手机号重复: " + phone);
            }

            String email = m.getEmail() != null ? m.getEmail().trim().toLowerCase() : "";
            if (!StringUtils.hasText(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员邮箱不能为空");
            }
            if (!seenEmails.add(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员邮箱重复: " + email);
            }

            String idCard = m.getIdCard() != null ? m.getIdCard().trim() : "";
            if (!StringUtils.hasText(idCard)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员身份证号不能为空");
            }
            if (!seenIdCards.add(idCard)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员身份证号重复: " + idCard);
            }

            if (!StringUtils.hasText(m.getCollege())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员学院不能为空");
            }
        }

        Map<String, User> verifiedLeaderUsers = validateAndResolveRegisteredUsers(competitionId, captain, leaderRows, "领队", null);
        Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, normalMemberRows, "队员", null);

        boolean requireAudit = config != null && Boolean.TRUE.equals(config.getRequireTeamAudit());
        CompetitionTeam team = CompetitionTeam.builder()
                .competition(competition)
                .teamName(request.getTeamName().trim())
                .avatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl() : "/api/v1/uploads/avatar/default_team.png")
                .captain(captain)
                .status(requireAudit ? TeamStatus.AUDIT_DRAFT : TeamStatus.AUDIT_PASSED)
                .build();

        CompetitionTeam savedTeam = teamRepository.save(team);

        CompetitionMember captainMember = CompetitionMember.builder()
                .competition(competition)
                .team(savedTeam)
                .user(captain)
                .role(MemberRole.CAPTAIN)
                .status(MemberStatus.INVITED_ACCEPTED)
                .realName(captainRow.getRealName().trim())
                .gender(captainRow.getGender() != null ? captainRow.getGender() : Gender.MALE)
                .studentNumber(captainRow.getStudentNumber().trim())
                .phone(captainRow.getPhone().trim())
                .email(captain.getEmail())
                .idCard(captainRow.getIdCard().trim())
                .college(captainRow.getCollege().trim())
                .build();
        memberRepository.save(captainMember);

        Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);
        for (TeamMemberSignupDTO l : leaderRows) {
            User leaderUser = verifiedLeaderUsers.get(l.getEmail().trim().toLowerCase());
            saveInvitedMemberAndNotify(competition, savedTeam, captain, leaderUser, l, expireTime);
        }

        for (TeamMemberSignupDTO m : normalMemberRows) {
            User memberUser = verifiedMemberUsers.get(m.getEmail().trim().toLowerCase());
            saveInvitedMemberAndNotify(competition, savedTeam, captain, memberUser, m, expireTime);
        }

        return savedTeam.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})
    public void updateTeam(Long competitionId, Long captainUserId, TeamUpdateRequestDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionTeam team = teamRepository.findByCompetitionIdAndCaptain_Id(competitionId, captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "您不是本场竞赛中任何队伍的队长，无权修改队伍信息"));

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);

        if (StringUtils.hasText(request.getAvatarUrl())) {
            team.setAvatarUrl(request.getAvatarUrl());
        }

        List<TeamMemberSignupDTO> memberList = request.getMembers();
        if (memberList != null && !memberList.isEmpty()) {
            int maxLeaders = config != null && config.getMaxLeaders() != null ? config.getMaxLeaders() : 1;
            int maxMembers = config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4;
            int minMembers = config != null && config.getMinTeamMembers() != null ? config.getMinTeamMembers() : 1;

            if (memberList.size() > maxMembers + maxLeaders) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "名册成员总行数超过上限 (" + (maxMembers + maxLeaders) + "行)");
            }

            List<TeamMemberSignupDTO> captainRows = new ArrayList<>();
            List<TeamMemberSignupDTO> leaderRows = new ArrayList<>();
            List<TeamMemberSignupDTO> normalMemberRows = new ArrayList<>();

            for (TeamMemberSignupDTO m : memberList) {
                if (m.getRole() == MemberRole.CAPTAIN) {
                    captainRows.add(m);
                } else if (m.getRole() == MemberRole.LEADER) {
                    leaderRows.add(m);
                } else if (m.getRole() == MemberRole.MEMBER) {
                    normalMemberRows.add(m);
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "未知的成员角色类型");
                }
            }

            if (captainRows.size() != 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍必须且只能包含 1 名队长");
            }
            TeamMemberSignupDTO captainRow = captainRows.getFirst();

            User captain = team.getCaptain();
            if (captain.getEmail() == null || !captain.getEmail().equalsIgnoreCase(captainRow.getEmail().trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队长邮箱必须为当前登录账号绑定的邮箱 (" + captain.getEmail() + ")");
            }

            if (leaderRows.size() > maxLeaders) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (最多 " + maxLeaders + " 人)");
            }
            int totalTeamMembers = 1 + normalMemberRows.size();
            if (totalTeamMembers > maxMembers) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队员人数超过上限 (" + maxMembers + "人，含队长)");
            }
            if (totalTeamMembers < minMembers) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍总人数未达到最低要求 (" + minMembers + "人，含队长)");
            }

            // 查重校验
            Set<String> seenEmails = new HashSet<>();
            Set<String> seenPhones = new HashSet<>();
            Set<String> seenIdCards = new HashSet<>();
            Set<String> seenStudentNumbers = new HashSet<>();
            for (TeamMemberSignupDTO m : memberList) {
                String studentNumber = m.getStudentNumber() != null ? m.getStudentNumber().trim() : "";
                if (!StringUtils.hasText(studentNumber)) throw new BusinessException(HttpStatus.BAD_REQUEST, "成员学号不能为空");
                if (!seenStudentNumbers.add(studentNumber)) throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员学号重复: " + studentNumber);

                String ph = m.getPhone() != null ? m.getPhone().trim() : "";
                if (!StringUtils.hasText(ph)) throw new BusinessException(HttpStatus.BAD_REQUEST, "成员手机号不能为空");
                if (!seenPhones.add(ph)) throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员手机号重复: " + ph);

                String em = m.getEmail() != null ? m.getEmail().trim().toLowerCase() : "";
                if (!StringUtils.hasText(em)) throw new BusinessException(HttpStatus.BAD_REQUEST, "成员邮箱不能为空");
                if (!seenEmails.add(em)) throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员邮箱重复: " + em);

                String idCard = m.getIdCard() != null ? m.getIdCard().trim() : "";
                if (!StringUtils.hasText(idCard)) throw new BusinessException(HttpStatus.BAD_REQUEST, "成员身份证号不能为空");
                if (!seenIdCards.add(idCard)) throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员身份证号重复: " + idCard);

                if (!StringUtils.hasText(m.getCollege())) throw new BusinessException(HttpStatus.BAD_REQUEST, "成员学院不能为空");
            }

            // 更新队长自身的登记明细
            CompetitionMember captainMember = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, team.getId(), captainUserId).orElse(null);
            if (captainMember != null) {
                if (StringUtils.hasText(captainRow.getRealName())) captainMember.setRealName(captainRow.getRealName().trim());
                if (captainRow.getGender() != null) captainMember.setGender(captainRow.getGender());
                if (StringUtils.hasText(captainRow.getStudentNumber())) captainMember.setStudentNumber(captainRow.getStudentNumber().trim());
                if (StringUtils.hasText(captainRow.getPhone())) captainMember.setPhone(captainRow.getPhone().trim());
                if (StringUtils.hasText(captainRow.getIdCard())) captainMember.setIdCard(captainRow.getIdCard().trim());
                if (StringUtils.hasText(captainRow.getCollege())) captainMember.setCollege(captainRow.getCollege().trim());
                memberRepository.save(captainMember);
            }

            Map<String, User> verifiedLeaderUsers = validateAndResolveRegisteredUsers(competitionId, captain, leaderRows, "领队", team.getId());
            Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, normalMemberRows, "队员", team.getId());

            List<CompetitionMember> currentMembers = memberRepository.findByTeamId(team.getId());
            Map<Long, CompetitionMember> currentMemberMap = currentMembers.stream()
                    .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, b) -> a));

            Set<Long> newTargetUserIds = new HashSet<>();
            verifiedLeaderUsers.values().forEach(u -> newTargetUserIds.add(u.getId()));
            verifiedMemberUsers.values().forEach(u -> newTargetUserIds.add(u.getId()));

            // 移除不再新名单中的未确认邀请或成员
            for (CompetitionMember m : currentMembers) {
                if (m.getRole() != MemberRole.CAPTAIN && !newTargetUserIds.contains(m.getUser().getId())) {
                    memberRepository.delete(m);
                }
            }

            Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);

            // 更新或创建领队
            for (TeamMemberSignupDTO l : leaderRows) {
                User user = verifiedLeaderUsers.get(l.getEmail().trim().toLowerCase());
                CompetitionMember existing = currentMemberMap.get(user.getId());
                if (existing != null) {
                    existing.setRealName(l.getRealName().trim());
                    existing.setGender(l.getGender() != null ? l.getGender() : Gender.MALE);
                    existing.setStudentNumber(l.getStudentNumber().trim());
                    existing.setPhone(l.getPhone().trim());
                    existing.setEmail(l.getEmail().trim());
                    existing.setIdCard(l.getIdCard().trim());
                    existing.setCollege(l.getCollege().trim());
                    existing.setRole(MemberRole.LEADER);
                    memberRepository.save(existing);
                } else {
                    saveInvitedMemberAndNotify(competition, team, captain, user, l, expireTime);
                }
            }

            // 更新或创建普通队员
            for (TeamMemberSignupDTO m : normalMemberRows) {
                User user = verifiedMemberUsers.get(m.getEmail().trim().toLowerCase());
                CompetitionMember existing = currentMemberMap.get(user.getId());
                if (existing != null) {
                    existing.setRealName(m.getRealName().trim());
                    existing.setGender(m.getGender() != null ? m.getGender() : Gender.MALE);
                    existing.setStudentNumber(m.getStudentNumber().trim());
                    existing.setPhone(m.getPhone().trim());
                    existing.setEmail(m.getEmail().trim());
                    existing.setIdCard(m.getIdCard().trim());
                    existing.setCollege(m.getCollege().trim());
                    existing.setRole(MemberRole.MEMBER);
                    memberRepository.save(existing);
                } else {
                    saveInvitedMemberAndNotify(competition, team, captain, user, m, expireTime);
                }
            }
        }

        // 队伍信息修改后，强制重置为未审核状态 (AUDIT_DRAFT)，必须由管理员重新审核
        team.setStatus(TeamStatus.AUDIT_DRAFT);
        team.setAuditReason(null);
        teamRepository.save(team);
        log.info("[Competition-Team] 队伍 {} 修改了队伍信息，状态重置为未审核 (AUDIT_DRAFT)，须等待管理员审核通过", team.getTeamName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})
    public void deleteMyTeam(Long competitionId, Long captainUserId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionTeam team = teamRepository.findByCompetitionIdAndCaptain_Id(competitionId, captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "您不是该队伍的队长，无权解散队伍"));

        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        for (CompetitionMember m : members) {
            if (m.getUser() != null && !Objects.equals(m.getUser().getId(), captainUserId)) {
                notificationService.updateActionStatus(
                        m.getUser().getId(),
                        NotificationType.COMPETITION_INVITE,
                        "\"teamId\":" + team.getId(),
                        NotificationActionStatus.CANCELLED
                );

                eventPublisher.publishEvent(SendNotificationEvent.of(
                        this,
                        captainUserId,
                        m.getUser().getId(),
                        NotificationType.COMPETITION_INVITE,
                        "【队伍解散通知】战队「" + team.getTeamName() + "」已被解散",
                        String.format("队长已解散战队「%s」（竞赛: 「%s」），相关参赛关系已解除。",
                                team.getTeamName(), competition.getTitle())
                ));
            }
        }

        memberRepository.deleteByTeamId(team.getId());
        teamRepository.delete(team);
        log.info("[Competition-Team] 队长 {} 解散了战队 {}", captainUserId, team.getTeamName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})
    public void acceptInvitation(Long competitionId, Long teamId, Long userId, TeamInviteAcceptDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        CompetitionMember member = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您未收到该队伍的邀请或邀请不存在"));

        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该邀请已处理或状态已变更: " + member.getStatus().getDescription());
        }

        // 严格冲突校验：一个用户对一个竞赛只能存在一个队伍，但是领队可以是多个队伍的领队
        List<CompetitionMember> otherAccepted = memberRepository.findAllByCompetitionIdAndUserId(competitionId, userId);
        boolean isAcceptingLeader = member.getRole() == MemberRole.LEADER;

        if (isAcceptingLeader) {
            // 领队不能在其他队伍中作为队长或普通队员参赛
            boolean alreadyPlaying = otherAccepted.stream()
                    .anyMatch(m -> !Objects.equals(m.getTeam().getId(), teamId)
                            && (m.getRole() == MemberRole.CAPTAIN || m.getRole() == MemberRole.MEMBER)
                            && m.getStatus() == MemberStatus.INVITED_ACCEPTED);
            if (alreadyPlaying) {
                throw new BusinessException(HttpStatus.CONFLICT, "您已在本次竞赛的其他队伍中作为参赛队员参赛，无法同时担任领队");
            }
        } else {
            // 普通队员/队长在全场竞赛只能属于一支队伍
            boolean alreadyInOtherTeam = otherAccepted.stream()
                    .anyMatch(m -> !Objects.equals(m.getTeam().getId(), teamId)
                            && m.getStatus() == MemberStatus.INVITED_ACCEPTED);
            if (alreadyInOtherTeam) {
                throw new BusinessException(HttpStatus.CONFLICT, "您已在本次竞赛的其他队伍中确认参赛，每名选手只能属于一支队伍");
            }

            if (teamRepository.existsByCompetitionIdAndCaptain_Id(competitionId, userId)) {
                throw new BusinessException(HttpStatus.CONFLICT, "您已是本次竞赛其他队伍的队长，无法接受此邀请");
            }
        }

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        if (isAcceptingLeader) {
            int maxLeaders = config != null && config.getMaxLeaders() != null ? config.getMaxLeaders() : 1;
            long currentAcceptedLeaders = memberRepository.findByTeamIdAndStatus(teamId, MemberStatus.INVITED_ACCEPTED).stream()
                    .filter(m -> m.getRole() == MemberRole.LEADER).count();
            if (currentAcceptedLeaders >= maxLeaders) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍领队人数已达上限 (" + maxLeaders + "人)");
            }
        } else {
            int maxMembers = config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4;
            long currentAcceptedMembers = memberRepository.findByTeamIdAndStatus(teamId, MemberStatus.INVITED_ACCEPTED).stream()
                    .filter(m -> m.getRole() != MemberRole.LEADER).count();
            if (currentAcceptedMembers >= maxMembers) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍队员人数已达上限 (" + maxMembers + "人，含队长)");
            }
        }

        User user = member.getUser();
        member.setStatus(MemberStatus.INVITED_ACCEPTED);
        memberRepository.save(member);

        // 1. 同步将原邀请卡片状态更新为已接受 (ACCEPTED)
        notificationService.updateActionStatus(
                userId,
                NotificationType.COMPETITION_INVITE,
                "\"teamId\":" + teamId,
                NotificationActionStatus.ACCEPTED
        );

        // 2. 向队长发送入队确认反馈通知
        eventPublisher.publishEvent(SendNotificationEvent.of(
                this,
                userId,
                team.getCaptain().getId(),
                NotificationType.COMPETITION_INVITE,
                "【组队通知】" + user.getUsername() + " 已同意加入战队",
                String.format("用户 %s 已同意邀请，成功加入战队「%s」（竞赛: 「%s」）。",
                        user.getUsername(), team.getTeamName(), competition.getTitle())
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvitation(Long competitionId, Long teamId, Long userId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        CompetitionMember member = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您未收到该队伍的邀请或邀请不存在"));

        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该邀请已处理或状态已变更: " + member.getStatus().getDescription());
        }

        member.setStatus(MemberStatus.NOT_PARTICIPATE);
        memberRepository.save(member);

        // 1. 同步将原邀请卡片状态更新为已拒绝 (REJECTED)
        notificationService.updateActionStatus(
                userId,
                NotificationType.COMPETITION_INVITE,
                "\"teamId\":" + teamId,
                NotificationActionStatus.REJECTED
        );

        // 2. 向队长发送拒绝入队反馈通知
        eventPublisher.publishEvent(SendNotificationEvent.of(
                this,
                userId,
                team.getCaptain().getId(),
                NotificationType.COMPETITION_INVITE,
                "【组队通知】" + member.getUser().getUsername() + " 拒绝了入队邀请",
                String.format("用户 %s 拒绝了加入战队「%s」（竞赛: 「%s」）的邀请。",
                        member.getUser().getUsername(), team.getTeamName(), competition.getTitle())
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInvitation(Long competitionId, Long teamId, Long captainUserId, Long targetUserId) {
        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        if (!Objects.equals(team.getCaptain().getId(), captainUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有队长有权取消邀请");
        }

        CompetitionMember member = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标成员不存在或未在该队伍中"));

        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该成员已确认或已处理，无法取消邀请");
        }

        memberRepository.delete(member);

        // 1. 同步将原邀请卡片状态更新为已撤销 (CANCELLED)
        notificationService.updateActionStatus(
                targetUserId,
                NotificationType.COMPETITION_INVITE,
                "\"teamId\":" + teamId,
                NotificationActionStatus.CANCELLED
        );

        // 2. 向被邀请人发送邀请撤回通知
        eventPublisher.publishEvent(SendNotificationEvent.of(
                this,
                captainUserId,
                targetUserId,
                NotificationType.COMPETITION_INVITE,
                "【组队通知】战队邀请已撤回",
                String.format("队长 %s 撤回了战队「%s」（竞赛: 「%s」）的入队邀请。",
                        team.getCaptain().getUsername(), team.getTeamName(), team.getCompetition().getTitle())
        ));
    }

    @Override
    public List<TeamInvitationVO> getMyInvitations(Long competitionId, Long userId) {
        List<CompetitionMember> pendingMembers = memberRepository.findUserInvitations(competitionId, userId, MemberStatus.INVITED);
        List<TeamInvitationVO> result = new ArrayList<>();
        for (CompetitionMember m : pendingMembers) {
            result.add(TeamInvitationVO.builder()
                    .id(m.getId())
                    .competitionId(competitionId)
                    .competitionTitle(m.getCompetition() != null ? m.getCompetition().getTitle() : null)
                    .teamId(m.getTeam().getId())
                    .teamName(m.getTeam().getTeamName())
                    .captainId(m.getTeam().getCaptain() != null ? m.getTeam().getCaptain().getId() : null)
                    .captainName(m.getTeam().getCaptain() != null ? m.getTeam().getCaptain().getUsername() : null)
                    .role(m.getRole())
                    .status(m.getStatus())
                    .invitedAt(m.getJoinedAt())
                    .realName(m.getRealName())
                    .email(m.getEmail())
                    .phone(m.getPhone())
                    .build());
        }
        return result;
    }

    @Override
    public MyTeamInfoVO getMyTeam(Long competitionId, Long userId) {
        List<CompetitionMember> memberships = memberRepository.findMembersWithTeam(competitionId, userId);
        if (memberships.isEmpty()) {
            return null;
        }

        // 优先寻找已确认的归属队伍（优先 CAPTAIN，次之 MEMBER，最后 LEADER）
        CompetitionMember member = memberships.stream()
                .filter(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED)
                .min(Comparator.comparingInt(m -> {
                    if (m.getRole() == MemberRole.CAPTAIN) return 0;
                    if (m.getRole() == MemberRole.MEMBER) return 1;
                    return 2;
                }))
                .orElse(null);

        if (member == null) {
            return null;
        }

        CompetitionTeam team = member.getTeam();
        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        // 稳定排序：队长 (CAPTAIN) 始终置顶，普通队员居中，领队随后，同角色按 ID 升序
        members.sort(Comparator.comparingInt((CompetitionMember m) -> {
            if (m.getRole() == MemberRole.CAPTAIN) return 0;
            if (m.getRole() == MemberRole.MEMBER) return 1;
            return 2;
        }).thenComparing(CompetitionMember::getId));

        List<CompetitionMemberVO> memberVOs = competitionMapper.toMemberVOList(members);

        return MyTeamInfoVO.builder()
                .teamId(team.getId())
                .competitionId(competitionId)
                .competitionTitle(team.getCompetition() != null ? team.getCompetition().getTitle() : null)
                .teamName(team.getTeamName())
                .avatarUrl(team.getAvatarUrl())
                .status(team.getStatus())
                .auditReason(team.getAuditReason())
                .myRole(member.getRole())
                .isCaptain(member.getRole() == MemberRole.CAPTAIN)
                .members(memberVOs)
                .myRegistrationData(Collections.emptyMap())
                .createdAt(team.getCreatedAt())
                .build();
    }

    @Override
    public List<CompetitionTeamVO> listTeams(Long competitionId, Long currentUserId) {
        if (!competitionRepository.existsById(competitionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在");
        }

        boolean isAdmin = checkIsAdmin();

        List<CompetitionMember> userMemberships = currentUserId != null
                ? memberRepository.findAllByCompetitionIdAndUserId(competitionId, currentUserId)
                : Collections.emptyList();

        Map<Long, MemberRole> myTeamRoleMap = userMemberships.stream()
                .filter(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED && m.getTeam() != null)
                .collect(Collectors.toMap(
                        m -> m.getTeam().getId(),
                        CompetitionMember::getRole,
                        (existing, replacement) -> existing
                ));

        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED);
        List<CompetitionTeamVO> result = new ArrayList<>(teams.size());

        for (CompetitionTeam team : teams) {
            CompetitionTeamVO vo = competitionMapper.toTeamVO(team);

            MemberRole myRole = myTeamRoleMap.get(team.getId());
            boolean isCaptain = (myRole == MemberRole.CAPTAIN)
                    || (team.getCaptain() != null && Objects.equals(team.getCaptain().getId(), currentUserId));
            boolean isLeader = (myRole == MemberRole.LEADER);
            boolean isCaptainOrLeader = isAdmin || isCaptain || isLeader;
            boolean isTeamMember = (myRole == MemberRole.MEMBER);

            List<CompetitionMember> members = new ArrayList<>(memberRepository.findByTeamId(team.getId()));
            members.sort(Comparator.comparingInt((CompetitionMember m) -> {
                if (m.getRole() == MemberRole.CAPTAIN) return 0;
                if (m.getRole() == MemberRole.MEMBER) return 1;
                return 2;
            }).thenComparing(CompetitionMember::getId));

            List<CompetitionMemberVO> memberVOs = new ArrayList<>(members.size());
            for (CompetitionMember member : members) {
                CompetitionMemberVO memberVO = competitionMapper.toMemberVO(member);
                Long memberUserId = member.getUser() != null ? member.getUser().getId() : null;

                if (isCaptainOrLeader) {
                    // 仅队长和领队（及管理员）能看到队员的完整信息（不脱敏）
                } else if (isTeamMember) {
                    // 队员之间对关键信息进行脱敏（例如 idCard, phone）
                    if (!Objects.equals(memberUserId, currentUserId)) {
                        desensitizeKeyInfo(memberVO);
                    }
                } else {
                    // 非本队成员（外部人员）：隐藏私密关键信息，仅保留公共基础信息
                    hidePrivateInfo(memberVO);
                }
                memberVOs.add(memberVO);
            }
            vo.setMembers(memberVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<CompetitionTeamVO> listTeams(Long competitionId) {
        return listTeams(competitionId, null);
    }

    private boolean checkIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String authority = a.getAuthority();
            return "ROLE_ADMIN".equals(authority) || "ROLE_SUPER_ADMIN".equals(authority) || "ROLE_SUPERADMIN".equals(authority);
        });
    }

    private void desensitizeKeyInfo(CompetitionMemberVO vo) {
        if (vo == null) return;
        vo.setIdCard(DesensitizationUtil.maskIdCard(vo.getIdCard()));
        vo.setPhone(DesensitizationUtil.maskPhone(vo.getPhone()));
        vo.setEmail(DesensitizationUtil.maskEmail(vo.getEmail()));
    }

    private void hidePrivateInfo(CompetitionMemberVO vo) {
        if (vo == null) return;
        vo.setIdCard(null);
        vo.setPhone(null);
        vo.setEmail(null);
        vo.setRealName(null);
        vo.setStudentNumber(null);
        vo.setCollege(null);
    }

    private Map<String, User> validateAndResolveRegisteredUsers(
            Long competitionId, User captain, List<TeamMemberSignupDTO> memberInputs, String roleLabel, Long currentTeamId) {
        Map<String, User> resultMap = new HashMap<>();

        for (TeamMemberSignupDTO input : memberInputs) {
            String email = input.getEmail() != null ? input.getEmail().trim().toLowerCase() : "";
            if (!StringUtils.hasText(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, roleLabel + "邮箱不能为空");
            }

            if (captain.getEmail() != null && captain.getEmail().equalsIgnoreCase(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不能填写队长自身的邮箱作为" + roleLabel);
            }

            // 1. 存活性验证 (通过邮箱查找平台已注册用户)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            roleLabel + "邮箱 [" + email + "] 尚未在平台注册账号，请通知其先注册账号"));

            // 2. 冲突校验
            // 规则：一个用户对一个竞赛只能存在一个队伍，但是领队可以是多个队伍的领队
            List<CompetitionMember> existingMembers = memberRepository.findAllByCompetitionIdAndUserId(competitionId, user.getId());
            boolean isTargetLeader = input.getRole() == MemberRole.LEADER;

            if (isTargetLeader) {
                // 如果当前作为领队加入：不能已经在其他队伍中作为队长或普通队员参赛 (INVITED_ACCEPTED)
                boolean alreadyPlaying = existingMembers.stream()
                        .anyMatch(m -> (currentTeamId == null || !m.getTeam().getId().equals(currentTeamId))
                                && (m.getRole() == MemberRole.CAPTAIN || m.getRole() == MemberRole.MEMBER)
                                && m.getStatus() == MemberStatus.INVITED_ACCEPTED);
                if (alreadyPlaying) {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "用户 [" + user.getUsername() + " (" + email + ")] 已作为队员/队长在本次竞赛中参赛，无法同时担任领队");
                }
            } else {
                // 如果当前作为普通队员加入：不能已经在其他队伍中且状态为 INVITED_ACCEPTED
                boolean alreadyInOtherTeam = existingMembers.stream()
                        .anyMatch(m -> (currentTeamId == null || !m.getTeam().getId().equals(currentTeamId))
                                && m.getStatus() == MemberStatus.INVITED_ACCEPTED);
                if (alreadyInOtherTeam) {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "队员 [" + user.getUsername() + " (" + email + ")] 已在其他队伍确认参赛，每名选手在同一竞赛中只能属于一支队伍");
                }
                if (teamRepository.existsByCompetitionIdAndCaptain_Id(competitionId, user.getId())) {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "队员 [" + user.getUsername() + " (" + email + ")] 已是本次竞赛其他队伍的队长，无法重复加入");
                }
            }

            resultMap.put(email, user);
        }

        return resultMap;
    }

    private void saveInvitedMemberAndNotify(
            Competition competition, CompetitionTeam team, User captain, User invitee,
            TeamMemberSignupDTO input, Instant expireTime) {
        CompetitionMember member = CompetitionMember.builder()
                .competition(competition)
                .team(team)
                .user(invitee)
                .role(input.getRole())
                .status(MemberStatus.INVITED)
                .realName(input.getRealName().trim())
                .gender(input.getGender() != null ? input.getGender() : Gender.MALE)
                .studentNumber(input.getStudentNumber().trim())
                .phone(input.getPhone().trim())
                .email(input.getEmail().trim().toLowerCase())
                .idCard(input.getIdCard().trim())
                .college(input.getCollege().trim())
                .build();
        memberRepository.save(member);

        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("competitionId", competition.getId());
        if (competition.getSlug() != null) {
            actionContext.put("slug", competition.getSlug());
        }
        actionContext.put("teamId", team.getId());
        actionContext.put("teamName", team.getTeamName());
        actionContext.put("inviterName", captain.getUsername());
        actionContext.put("memberRole", input.getRole().name());

        String roleName = input.getRole() == MemberRole.LEADER ? "领队" : "队员";
        String content = String.format("用户 %s 邀请您作为【%s】加入战队「%s」共同参加竞赛「%s」，请及时确认是否参赛。",
                captain.getUsername(), roleName, team.getTeamName(), competition.getTitle());

        eventPublisher.publishEvent(SendNotificationEvent.builder()
                .source(this)
                .senderId(captain.getId())
                .receiverId(invitee.getId())
                .type(NotificationType.COMPETITION_INVITE)
                .title("【竞赛邀请】" + captain.getUsername() + " 邀请您加入战队「" + team.getTeamName() + "」")
                .content(content)
                .actionStatus(NotificationActionStatus.PENDING)
                .actionData(JSON.toJSONString(actionContext))
                .expireTime(expireTime)
                .build());
    }

}
