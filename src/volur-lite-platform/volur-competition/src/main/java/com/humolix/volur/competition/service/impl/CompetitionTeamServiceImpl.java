package com.humolix.volur.competition.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.*;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.*;
import com.humolix.volur.competition.domain.record.TeamLimits;
import com.humolix.volur.competition.domain.vo.CompetitionMemberVO;
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
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.Gender;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionTeamServiceImpl implements CompetitionTeamService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CompetitionChatMessageRepository chatMessageRepository;
    private final CompetitionTeamInvitationRepository teamInvitationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

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

        TeamLimits limits = TeamLimits.from(config);

        List<TeamMemberSignupDTO> memberList = request.getMembers() != null ? request.getMembers() : Collections.emptyList();
        if (memberList.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛成员名册不能为空");
        }
        if (memberList.size() > limits.maxMembers() + limits.maxLeaders()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "名册成员总行数超过上限 (" + (limits.maxMembers() + limits.maxLeaders()) + "行)");
        }

        List<TeamMemberSignupDTO> captains = new ArrayList<>();
        List<TeamMemberSignupDTO> members = new ArrayList<>();

        for (TeamMemberSignupDTO m : memberList) {
            switch (m.getRole()) {
                case CAPTAIN -> captains.add(m);
                case MEMBER  -> members.add(m);
                case null, default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛成员列表中只能包含队长和队员");
            }
        }

        if (captains.size() != 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍必须且只能包含 1 名队长");
        }
        TeamMemberSignupDTO captainRow = captains.getFirst();

        List<TeamLeadersSignupDTO> leaderList = request.getLeaders() != null ? request.getLeaders() : Collections.emptyList();
        if (leaderList.size() > limits.maxLeaders()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (最多 " + limits.maxLeaders() + " 人)");
        }

        int totalTeamMembers = 1 + members.size();
        if (totalTeamMembers > limits.maxMembers() || totalTeamMembers < limits.minMembers()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    String.format("参赛队员人数须在 %d~%d 人之间（含队长）", limits.minMembers(), limits.maxMembers()));
        }

        User captain = userRepository.findById(captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (captain.getEmail() == null || !captain.getEmail().equalsIgnoreCase(captainRow.getEmail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队长邮箱必须为当前登录账号绑定的邮箱 (" + captain.getEmail() + ")");
        }

        // 统一名册字段判空与防重校验 (区分学校与其他单位)
        boolean isEnterprise = request.getOrganizationType() != null && request.getOrganizationType() != OrganizationType.SCHOOL;
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenIdCards = new HashSet<>();
        Set<String> seenStudentNumbers = new HashSet<>();

        for (TeamMemberSignupDTO m : memberList) {
            String empNo = m.getEmployeeNumber() != null ? m.getEmployeeNumber() : "";
            if (!StringUtils.hasText(empNo)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员工号不能为空" : "成员学号不能为空");
            }
            if (!seenStudentNumbers.add(empNo)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, (isEnterprise ? "填写的成员工号重复: " : "填写的成员学号重复: ") + empNo);
            }

            String dept = m.getDepartment() != null ? m.getDepartment() : "";
            if (!StringUtils.hasText(dept)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员部门不能为空" : "成员学院不能为空");
            }

            String pos = m.getPosition() != null ? m.getPosition() : "";
            if (!StringUtils.hasText(pos)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员职位不能为空" : "成员班级不能为空");
            }

            String phone = m.getPhone() != null ? m.getPhone() : "";
            if (!StringUtils.hasText(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员手机号不能为空");
            }
            if (!seenPhones.add(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员手机号重复: " + phone);
            }

            String email = m.getEmail() != null ? m.getEmail() : "";
            if (!StringUtils.hasText(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员邮箱不能为空");
            }
            if (!seenEmails.add(email)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员邮箱重复: " + email);
            }

            String idCard = m.getIdCard() != null ? m.getIdCard().toUpperCase() : "";
            if (!StringUtils.hasText(idCard)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "成员身份证号不能为空");
            }
            if (!seenIdCards.add(idCard)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员身份证号重复: " + idCard);
            }
        }

        validateMemberIdentitiesAcrossTeams(competitionId, null, memberList);

        for (TeamLeadersSignupDTO l : leaderList) {
            if (!StringUtils.hasText(l.getRealName())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "领队姓名不能为空");
            }
            if (!StringUtils.hasText(l.getPhone())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "领队手机号不能为空");
            }
            if (StringUtils.hasText(l.getEmail()) && captain.getEmail() != null
                    && captain.getEmail().equalsIgnoreCase(l.getEmail())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不能填写队长自身的邮箱作为指导老师/领队");
            }
        }

        Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, members, "队员", null);

        boolean requireAudit = config != null && Boolean.TRUE.equals(config.getRequireTeamAudit());
        OrganizationType orgType = request.getOrganizationType() != null ? request.getOrganizationType() : OrganizationType.SCHOOL;
        CompetitionTeam team = CompetitionTeam.builder()
                .competition(competition)
                .teamName(request.getTeamName())
                .avatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl().trim() : null)
                .description(request.getDescription())
                .organization(request.getOrganization())
                .organizationType(orgType)
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
                .realName(captainRow.getRealName())
                .gender(captainRow.getGender() != null ? captainRow.getGender() : Gender.MALE)
                .phone(captainRow.getPhone())
                .email(captain.getEmail())
                .idCard(captainRow.getIdCard() != null ? captainRow.getIdCard().toUpperCase() : null)
                .employeeNumber(captainRow.getEmployeeNumber())
                .department(captainRow.getDepartment())
                .position(captainRow.getPosition())
                .build();
        memberRepository.save(captainMember);

        // 保存领队/指导老师（无需注册、无需邀请接受，直接确认为 INVITED_ACCEPTED）
        for (TeamLeadersSignupDTO l : leaderList) {
            User leaderUser = null;
            if (StringUtils.hasText(l.getEmail())) {
                leaderUser = userRepository.findByEmail(l.getEmail()).orElse(null);
            }
            CompetitionMember leaderMember = CompetitionMember.builder()
                    .competition(competition)
                    .team(savedTeam)
                    .user(leaderUser)
                    .role(MemberRole.LEADER)
                    .status(MemberStatus.INVITED_ACCEPTED)
                    .realName(l.getRealName())
                    .gender(l.getGender() != null ? l.getGender() : Gender.MALE)
                    .phone(l.getPhone())
                    .email(l.getEmail())
                    .idCard(StringUtils.hasText(l.getIdCard()) ? l.getIdCard().toUpperCase() : null)
                    .employeeNumber(l.getEmployeeNumber())
                    .department(l.getDepartment())
                    .position(l.getPosition())
                    .build();
            memberRepository.save(leaderMember);
        }

        Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);
        for (TeamMemberSignupDTO m : members) {
            User memberUser = verifiedMemberUsers.get(m.getEmail());
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

        if (request.getAvatarUrl() != null) {
            team.setAvatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl().trim() : null);
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getOrganization())) {
            team.setOrganization(request.getOrganization().trim());
        }
        if (request.getOrganizationType() != null) {
            team.setOrganizationType(request.getOrganizationType());
        }

        TeamLimits limits = TeamLimits.from(config);
        OrganizationType currentOrgType = request.getOrganizationType() != null ? request.getOrganizationType() : team.getOrganizationType();
        boolean isEnterprise = currentOrgType != null && currentOrgType != OrganizationType.SCHOOL;

        List<TeamMemberSignupDTO> memberList = request.getMembers();
        if (memberList != null && !memberList.isEmpty()) {
            List<TeamMemberSignupDTO> captains = new ArrayList<>();
            List<TeamMemberSignupDTO> members = new ArrayList<>();

            for (TeamMemberSignupDTO m : memberList) {
                switch (m.getRole()) {
                    case CAPTAIN -> captains.add(m);
                    case MEMBER  -> members.add(m);
                    case null, default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "参赛成员列表中只能包含队长和队员");
                }
            }

            if (captains.size() != 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍必须且只能包含 1 名队长");
            }
            TeamMemberSignupDTO captainRow = captains.getFirst();

            User captain = team.getCaptain();
            if (captain.getEmail() == null || !captain.getEmail().equalsIgnoreCase(captainRow.getEmail().trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "队长邮箱必须为当前登录账号绑定的邮箱 (" + captain.getEmail() + ")");
            }

            int totalTeamMembers = 1 + members.size();
            if (totalTeamMembers > limits.maxMembers() || totalTeamMembers < limits.minMembers()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        String.format("参赛队员人数须在 %d~%d 人之间（含队长）", limits.minMembers(), limits.maxMembers()));
            }

            // 统一名册字段判空与防重校验 (还原之前独立校验逻辑，区分学校与企业)
            Set<String> seenEmails = new HashSet<>();
            Set<String> seenPhones = new HashSet<>();
            Set<String> seenIdCards = new HashSet<>();
            Set<String> seenStudentNumbers = new HashSet<>();

            for (TeamMemberSignupDTO m : memberList) {
                String empNo = m.getEmployeeNumber() != null ? m.getEmployeeNumber() : "";
                if (!StringUtils.hasText(empNo)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员工号不能为空" : "成员学号不能为空");
                }
                if (!seenStudentNumbers.add(empNo)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, (isEnterprise ? "填写的成员工号重复: " : "填写的成员学号重复: ") + empNo);
                }

                String dept = m.getDepartment() != null ? m.getDepartment() : "";
                if (!StringUtils.hasText(dept)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员部门不能为空" : "成员学院不能为空");
                }

                String pos = m.getPosition() != null ? m.getPosition() : "";
                if (!StringUtils.hasText(pos)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, isEnterprise ? "成员职位不能为空" : "成员班级不能为空");
                }

                String phone = m.getPhone() != null ? m.getPhone() : "";
                if (!StringUtils.hasText(phone)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "成员手机号不能为空");
                }
                if (!seenPhones.add(phone)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员手机号重复: " + phone);
                }

                String email = m.getEmail() != null ? m.getEmail() : "";
                if (!StringUtils.hasText(email)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "成员邮箱不能为空");
                }
                if (!seenEmails.add(email)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员邮箱重复: " + email);
                }

                String idCard = m.getIdCard() != null ? m.getIdCard().toUpperCase() : "";
                if (!StringUtils.hasText(idCard)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "成员身份证号不能为空");
                }
                if (!seenIdCards.add(idCard)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的成员身份证号重复: " + idCard);
                }
            }

            // 跨队伍唯一性冲突校验（排除当前战队自身，允许战队内部修改）
            validateMemberIdentitiesAcrossTeams(competitionId, team.getId(), memberList);

            // 更新队长自身的登记明细
            CompetitionMember captainMember = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, team.getId(), captainUserId).orElse(null);
            if (captainMember != null) {
                if (StringUtils.hasText(captainRow.getRealName())) captainMember.setRealName(captainRow.getRealName());
                if (captainRow.getGender() != null) captainMember.setGender(captainRow.getGender());
                if (StringUtils.hasText(captainRow.getPhone())) captainMember.setPhone(captainRow.getPhone());
                if (StringUtils.hasText(captainRow.getIdCard())) captainMember.setIdCard(captainRow.getIdCard().toUpperCase());
                captainMember.setEmployeeNumber(captainRow.getEmployeeNumber());
                captainMember.setDepartment(captainRow.getDepartment());
                captainMember.setPosition(captainRow.getPosition());
                memberRepository.save(captainMember);
            }

            Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, members, "队员", team.getId());

            List<CompetitionMember> currentMembers = memberRepository.findByTeamId(team.getId());
            Map<Long, CompetitionMember> currentMemberMap = currentMembers.stream()
                    .filter(m -> m.getUser() != null)
                    .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, b) -> a));

            Set<Long> newMemberUserIds = new HashSet<>();
            verifiedMemberUsers.values().forEach(u -> newMemberUserIds.add(u.getId()));

            // 移除不再新名单中的未确认邀请或队员
            for (CompetitionMember m : currentMembers) {
                if (m.getRole() == MemberRole.MEMBER && m.getUser() != null && !newMemberUserIds.contains(m.getUser().getId())) {
                    memberRepository.delete(m);
                }
            }

            Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);

            // 更新或创建普通队员
            for (TeamMemberSignupDTO m : members) {
                User user = verifiedMemberUsers.get(m.getEmail());
                CompetitionMember existing = currentMemberMap.get(user.getId());
                if (existing != null) {
                    existing.setRealName(m.getRealName());
                    existing.setGender(m.getGender() != null ? m.getGender() : Gender.MALE);
                    existing.setPhone(m.getPhone());
                    existing.setEmail(m.getEmail());
                    existing.setIdCard(StringUtils.hasText(m.getIdCard()) ? m.getIdCard().toUpperCase() : null);
                    existing.setEmployeeNumber(m.getEmployeeNumber());
                    existing.setDepartment(m.getDepartment());
                    existing.setPosition(m.getPosition());
                    existing.setRole(MemberRole.MEMBER);
                    memberRepository.save(existing);
                } else {
                    saveInvitedMemberAndNotify(competition, team, captain, user, m, expireTime);
                }
            }
        }

        // 更新指导老师 / 领队
        if (request.getLeaders() != null) {
            List<TeamLeadersSignupDTO> leaderList = request.getLeaders();
            if (leaderList.size() > limits.maxLeaders()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (最多 " + limits.maxLeaders() + " 人)");
            }
            User captain = team.getCaptain();
            for (TeamLeadersSignupDTO l : leaderList) {
                if (!StringUtils.hasText(l.getRealName())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "领队姓名不能为空");
                }
                if (!StringUtils.hasText(l.getPhone())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "领队手机号不能为空");
                }
                if (StringUtils.hasText(l.getEmail()) && captain.getEmail() != null
                        && captain.getEmail().equalsIgnoreCase(l.getEmail())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "不能填写队长自身的邮箱作为指导老师/领队");
                }
            }
            List<CompetitionMember> currentMembers = memberRepository.findByTeamId(team.getId());
            List<CompetitionMember> oldLeaders = currentMembers.stream().filter(m -> m.getRole() == MemberRole.LEADER).toList();
            if (!oldLeaders.isEmpty()) {
                memberRepository.deleteAll(oldLeaders);
            }
            for (TeamLeadersSignupDTO l : leaderList) {
                User leaderUser = null;
                if (StringUtils.hasText(l.getEmail())) {
                    leaderUser = userRepository.findByEmail(l.getEmail()).orElse(null);
                }
                CompetitionMember leaderMember = CompetitionMember.builder()
                        .competition(competition)
                        .team(team)
                        .user(leaderUser)
                        .role(MemberRole.LEADER)
                        .status(MemberStatus.INVITED_ACCEPTED)
                        .realName(l.getRealName())
                        .gender(l.getGender() != null ? l.getGender() : Gender.MALE)
                        .phone(l.getPhone())
                        .email(l.getEmail())
                        .idCard(StringUtils.hasText(l.getIdCard()) ? l.getIdCard().toUpperCase() : null)
                        .employeeNumber(l.getEmployeeNumber())
                        .department(l.getDepartment())
                        .position(l.getPosition())
                        .build();
                memberRepository.save(leaderMember);
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

        teamInvitationRepository.deleteByTeamId(team.getId());
        chatMessageRepository.deleteByTeamId(team.getId());
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
    @Transactional(rollbackFor = Exception.class)
    public void reinviteMember(Long competitionId, Long teamId, Long captainUserId, Long targetUserId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        if (!Objects.equals(team.getCaptain().getId(), captainUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只有队长有权重新发送入队邀请");
        }

        CompetitionMember member = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标成员不存在或未在该队伍中"));

        if (member.getStatus() == MemberStatus.INVITED_ACCEPTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该成员已接受邀请，无需重新邀请");
        }

        // 检查该用户是否已在其他队伍确认参赛
        List<CompetitionMember> existingMembers = memberRepository.findAllByCompetitionIdAndUserId(competitionId, targetUserId);
        boolean alreadyInOtherTeam = existingMembers.stream()
                .anyMatch(m -> !Objects.equals(m.getTeam().getId(), teamId) && m.getStatus() == MemberStatus.INVITED_ACCEPTED);
        if (alreadyInOtherTeam) {
            throw new BusinessException(HttpStatus.CONFLICT, "该用户已在其他队伍确认参赛，无法重新邀请");
        }

        member.setStatus(MemberStatus.INVITED);
        member.setJoinedAt(Instant.now());
        memberRepository.save(member);

        // 同步将历史邀请卡片状态重置或置为已撤销后重新发送
        notificationService.updateActionStatus(
                targetUserId,
                NotificationType.COMPETITION_INVITE,
                "\"teamId\":" + teamId,
                NotificationActionStatus.CANCELLED
        );

        Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("competitionId", competition.getId());
        if (competition.getSlug() != null) {
            actionContext.put("slug", competition.getSlug());
        }
        actionContext.put("teamId", team.getId());
        actionContext.put("teamName", team.getTeamName());
        actionContext.put("inviterName", team.getCaptain().getUsername());
        actionContext.put("memberRole", member.getRole().name());

        String roleName = member.getRole() == MemberRole.LEADER ? "领队" : "队员";
        String content = String.format("队长 %s 重新邀请您作为【%s】加入战队「%s」（竞赛: 「%s」），请及时确认是否参赛。",
                team.getCaptain().getUsername(), roleName, team.getTeamName(), competition.getTitle());

        eventPublisher.publishEvent(SendNotificationEvent.builder()
                .source(this)
                .senderId(captainUserId)
                .receiverId(targetUserId)
                .type(NotificationType.COMPETITION_INVITE)
                .title("【竞赛邀请】" + team.getCaptain().getUsername() + " 重新邀请您加入战队「" + team.getTeamName() + "」")
                .content(content)
                .actionStatus(NotificationActionStatus.PENDING)
                .actionData(serializeActionContext(actionContext))
                .expireTime(expireTime)
                .build());

        log.info("[Competition-Team] 队长 {} 重新邀请了成员 {} (队伍: {})",
                captainUserId, targetUserId, team.getTeamName());
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
        List<CompetitionMember> members = memberRepository.findByTeamIdWithUserAndProfile(team.getId());
        // 稳定排序：队长 (CAPTAIN) 始终置顶，普通队员居中，领队随后，同角色按 ID 升序
        members.sort(Comparator.comparingInt((CompetitionMember m) -> {
            if (m.getRole() == MemberRole.CAPTAIN) return 0;
            if (m.getRole() == MemberRole.MEMBER) return 1;
            return 2;
        }).thenComparing(CompetitionMember::getId));

        List<CompetitionMemberVO> memberVOs = competitionMapper.toMemberVOList(members);
        populateMemberAvatars(memberVOs, members);

        return MyTeamInfoVO.builder()
                .teamId(team.getId())
                .competitionId(competitionId)
                .competitionTitle(team.getCompetition() != null ? team.getCompetition().getTitle() : null)
                .teamName(team.getTeamName())
                .avatarUrl(team.getAvatarUrl())
                .description(team.getDescription())
                .organization(team.getOrganization())
                .organizationType(team.getOrganizationType() != null ? team.getOrganizationType() : OrganizationType.SCHOOL)
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
    public MyTeamInfoVO getTeamById(Long competitionId, Long teamId, Long userId) {
        CompetitionTeam team = teamRepository.findByIdWithCompetitionAndCaptain(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));
        if (!Objects.equals(team.getCompetition().getId(), competitionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "该队伍不属于当前竞赛");
        }
        List<CompetitionMember> members = memberRepository.findByTeamIdWithUserAndProfile(team.getId());
        members.sort(Comparator.comparingInt((CompetitionMember m) -> {
            if (m.getRole() == MemberRole.CAPTAIN) return 0;
            if (m.getRole() == MemberRole.MEMBER) return 1;
            return 2;
        }).thenComparing(CompetitionMember::getId));

        List<CompetitionMemberVO> memberVOs = competitionMapper.toMemberVOList(members);
        populateMemberAvatars(memberVOs, members);

        CompetitionMember myMembership = null;
        if (userId != null) {
            myMembership = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, userId).orElse(null);
        }

        return MyTeamInfoVO.builder()
                .teamId(team.getId())
                .competitionId(competitionId)
                .competitionTitle(team.getCompetition() != null ? team.getCompetition().getTitle() : null)
                .teamName(team.getTeamName())
                .avatarUrl(team.getAvatarUrl())
                .description(team.getDescription())
                .organization(team.getOrganization())
                .organizationType(team.getOrganizationType() != null ? team.getOrganizationType() : OrganizationType.SCHOOL)
                .status(team.getStatus())
                .auditReason(team.getAuditReason())
                .myRole(myMembership != null ? myMembership.getRole() : null)
                .isCaptain(myMembership != null && myMembership.getRole() == MemberRole.CAPTAIN)
                .members(memberVOs)
                .myRegistrationData(Collections.emptyMap())
                .createdAt(team.getCreatedAt())
                .build();
    }

    private Map<String, User> validateAndResolveRegisteredUsers(
            Long competitionId, User captain, List<TeamMemberSignupDTO> memberInputs, String roleLabel, Long currentTeamId) {
        Map<String, User> resultMap = new HashMap<>();

        for (TeamMemberSignupDTO input : memberInputs) {
            String email = input.getEmail() != null ? input.getEmail() : "";
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
        MemberRole role = input.getRole() != null ? input.getRole() : MemberRole.MEMBER;
        CompetitionMember member = CompetitionMember.builder()
                .competition(competition)
                .team(team)
                .user(invitee)
                .role(role)
                .status(MemberStatus.INVITED)
                .realName(input.getRealName())
                .gender(input.getGender() != null ? input.getGender() : Gender.MALE)
                .phone(input.getPhone())
                .email(input.getEmail())
                .idCard(input.getIdCard() != null ? input.getIdCard().toUpperCase() : null)
                .employeeNumber(input.getEmployeeNumber())
                .department(input.getDepartment())
                .position(input.getPosition())
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
        actionContext.put("memberRole", role.name());

        String roleName = role == MemberRole.LEADER ? "领队" : "队员";
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
                .actionData(serializeActionContext(actionContext))
                .expireTime(expireTime)
                .build());
    }

    private void validateMemberIdentitiesAcrossTeams(
            Long competitionId,
            Long excludeTeamId,
            List<TeamMemberSignupDTO> memberList) {

        if (memberList == null || memberList.isEmpty()) {
            return;
        }

        Set<String> phones = new HashSet<>();
        Set<String> idCards = new HashSet<>();
        Set<String> emails = new HashSet<>();

        for (TeamMemberSignupDTO m : memberList) {
            if (StringUtils.hasText(m.getPhone())) {
                phones.add(m.getPhone());
            }
            if (StringUtils.hasText(m.getIdCard())) {
                idCards.add(m.getIdCard().toUpperCase());
            }
            if (StringUtils.hasText(m.getEmail())) {
                emails.add(m.getEmail());
            }
        }

        if (phones.isEmpty() && idCards.isEmpty() && emails.isEmpty()) {
            return;
        }

        Collection<String> safePhones = phones.isEmpty() ? List.of("__EMPTY_PHONE__") : phones;
        Collection<String> safeIdCards = idCards.isEmpty() ? List.of("__EMPTY_IDCARD__") : idCards;
        Collection<String> safeEmails = emails.isEmpty() ? List.of("__EMPTY_EMAIL__") : emails;

        List<MemberStatus> activeStatuses = List.of(MemberStatus.INVITED, MemberStatus.INVITED_ACCEPTED);

        List<CompetitionMember> conflicts = memberRepository.findConflictingMembers(
                competitionId, activeStatuses, excludeTeamId, safePhones, safeIdCards, safeEmails);

        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }

        for (TeamMemberSignupDTO input : memberList) {
            String inputPhone = input.getPhone() != null ? input.getPhone() : "";
            String inputIdCard = input.getIdCard() != null ? input.getIdCard().toUpperCase() : "";
            String inputEmail = input.getEmail() != null ? input.getEmail() : "";
            String inputName = StringUtils.hasText(input.getRealName()) ? input.getRealName() : "未命名成员";
            boolean isInputLeader = input.getRole() == MemberRole.LEADER;

            for (CompetitionMember existing : conflicts) {
                if (excludeTeamId != null && existing.getTeam() != null && Objects.equals(existing.getTeam().getId(), excludeTeamId)) {
                    continue;
                }

                String existingPhone = existing.getPhone() != null ? existing.getPhone().trim() : "";
                String existingIdCard = existing.getIdCard() != null ? existing.getIdCard().trim().toUpperCase() : "";
                String existingEmail = existing.getEmail() != null ? existing.getEmail().trim().toLowerCase() : "";
                boolean isExistingLeader = existing.getRole() == MemberRole.LEADER;

                // 1. 手机号冲突校验
                if (StringUtils.hasText(inputPhone) && inputPhone.equals(existingPhone)) {
                    if (isInputLeader && isExistingLeader && inputEmail.equals(existingEmail)) {
                        // 允许同一个领队以相同账号带多队
                    } else {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                String.format("成员 [%s] 的手机号 [%s] 已在本场竞赛的其他队伍中登记，请勿重复使用",
                                        inputName, inputPhone));
                    }
                }

                // 2. 身份证号冲突校验
                if (StringUtils.hasText(inputIdCard) && inputIdCard.equals(existingIdCard)) {
                    if (isInputLeader && isExistingLeader && inputEmail.equals(existingEmail)) {
                        // 允许同一个领队以相同账号带多队
                    } else {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                String.format("成员 [%s] 的身份证号 [%s] 已在本场竞赛的其他队伍中登记，每名参赛者只能报名一支队伍",
                                        inputName, DesensitizationUtil.maskIdCard(inputIdCard)));
                    }
                }

                // 3. 邮箱冲突校验
                if (StringUtils.hasText(inputEmail) && inputEmail.equals(existingEmail)) {
                    if (isInputLeader && isExistingLeader) {
                        // 允许同一个领队带多队
                    } else {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                String.format("成员 [%s] 的邮箱 [%s] 已在本场竞赛的其他队伍中登记",
                                        inputName, inputEmail));
                    }
                }
            }
        }
    }

    private void populateMemberAvatars(List<CompetitionMemberVO> memberVOs, List<CompetitionMember> members) {
        if (memberVOs == null || memberVOs.isEmpty()) {
            return;
        }
        List<Long> userIds = members.stream()
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> avatarMap = userProfileRepository.findAllById(userIds).stream()
                .filter(p -> StringUtils.hasText(p.getAvatarUrl()))
                .collect(Collectors.toMap(UserProfile::getId, UserProfile::getAvatarUrl, (a, b) -> a));
        memberVOs.forEach(vo -> {
            if (vo.getUserId() != null) {
                vo.setAvatarUrl(avatarMap.get(vo.getUserId()));
            }
        });
    }

    private String serializeActionContext(Object context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            return "{}";
        }
    }

}
