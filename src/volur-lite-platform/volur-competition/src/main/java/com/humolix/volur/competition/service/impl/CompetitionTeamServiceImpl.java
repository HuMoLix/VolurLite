package com.humolix.volur.competition.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamMemberInputDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.entity.*;
import com.humolix.volur.competition.domain.enums.FillScope;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionMemberVO;
import com.humolix.volur.competition.domain.vo.CompetitionTeamVO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.event.TeamInvitationHandledEvent;
import com.humolix.volur.competition.event.TeamInvitationSentEvent;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.*;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionTeamServiceImpl implements CompetitionTeamService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionFieldSchemaRepository fieldSchemaRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CompetitionRegistrationDataRepository registrationDataRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long signupTeam(Long competitionId, Long captainUserId, TeamSignupRequestDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        validateRegistrationPeriod(competition);

        // 1. 队长资格检查
        Optional<CompetitionMember> existingMemberOpt = memberRepository.findByCompetitionIdAndUserId(competitionId, captainUserId);
        if (existingMemberOpt.isPresent() && existingMemberOpt.get().getStatus() == MemberStatus.INVITED_ACCEPTED) {
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

        List<TeamMemberInputDTO> leaders = request.getLeaders() != null ? request.getLeaders() : Collections.emptyList();
        List<TeamMemberInputDTO> members = request.getMembers() != null ? request.getMembers() : Collections.emptyList();

        if (leaders.size() > maxLeaders) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (" + maxLeaders + "人)");
        }

        int totalTeamMembers = 1 + members.size(); // 队长 + 队员
        if (totalTeamMembers > maxMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队员人数超过上限 (" + maxMembers + "人，含队长)");
        }
        if (totalTeamMembers < minMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍总人数未达到最低要求 (" + minMembers + "人)");
        }

        User captain = userRepository.findById(captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 2. 存活性验证与冲突验证
        Map<String, User> verifiedLeaderUsers = validateAndResolveRegisteredUsers(competitionId, captain, leaders, "领队");
        Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, members, "队员");

        // 3. 校验并保存队长动态表单数据
        validateAndSaveRegistrationData(competition, null, captain, request.getCaptainData(), MemberRole.CAPTAIN);

        // 4. 创建队伍持久化
        String inviteCode = RandomUtil.randomString(8).toUpperCase();
        CompetitionTeam team = CompetitionTeam.builder()
                .competition(competition)
                .teamName(request.getTeamName().trim())
                .teamSlogan(request.getTeamSlogan())
                .avatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl() : "/api/v1/uploads/avatar/default_team.png")
                .captain(captain)
                .inviteCode(inviteCode)
                .status(TeamStatus.AUDIT_DRAFT) // 初始创建为草稿/待审核状态
                .build();

        CompetitionTeam savedTeam = teamRepository.save(team);

        // 5. 保存队长成员记录 (INVITED_ACCEPTED)
        CompetitionMember captainMember = CompetitionMember.builder()
                .competition(competition)
                .team(savedTeam)
                .user(captain)
                .role(MemberRole.CAPTAIN)
                .status(MemberStatus.INVITED_ACCEPTED)
                .build();
        memberRepository.save(captainMember);

        // 6. 保存领队成员记录 (INVITED) 并发送站内信
        Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);
        for (TeamMemberInputDTO leaderInput : leaders) {
            User leaderUser = verifiedLeaderUsers.get(leaderInput.getPhone().trim());
            saveInvitedMemberAndNotify(competition, savedTeam, captain, leaderUser, MemberRole.LEADER, leaderInput.getRemark(), expireTime);
        }

        // 7. 保存队员成员记录 (INVITED) 并发送站内信
        for (TeamMemberInputDTO memberInput : members) {
            User memberUser = verifiedMemberUsers.get(memberInput.getPhone().trim());
            saveInvitedMemberAndNotify(competition, savedTeam, captain, memberUser, MemberRole.MEMBER, memberInput.getRemark(), expireTime);
        }

        // 8. 关联队长表单数据到 savedTeam
        List<CompetitionRegistrationData> dataList = registrationDataRepository.findByCompetitionIdAndUserId(competitionId, captainUserId);
        for (CompetitionRegistrationData d : dataList) {
            if (d.getTeam() == null) {
                d.setTeam(savedTeam);
                registrationDataRepository.save(d);
            }
        }

        return savedTeam.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeam(Long competitionId, Long captainUserId, TeamUpdateRequestDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        validateRegistrationPeriod(competition);

        CompetitionTeam team = teamRepository.findByCompetitionIdAndCaptain_Id(competitionId, captainUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "您不是本场竞赛中任何队伍的队长，无权修改队伍信息"));

        if (StringUtils.hasText(request.getTeamName()) && !request.getTeamName().trim().equals(team.getTeamName())) {
            if (teamRepository.existsByCompetitionIdAndTeamName(competitionId, request.getTeamName().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "队伍名称已存在");
            }
            team.setTeamName(request.getTeamName().trim());
        }

        if (request.getTeamSlogan() != null) {
            team.setTeamSlogan(request.getTeamSlogan());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            team.setAvatarUrl(request.getAvatarUrl());
        }

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        int maxLeaders = config != null && config.getMaxLeaders() != null ? config.getMaxLeaders() : 1;
        int maxMembers = config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4;

        List<TeamMemberInputDTO> leaders = request.getLeaders() != null ? request.getLeaders() : Collections.emptyList();
        List<TeamMemberInputDTO> members = request.getMembers() != null ? request.getMembers() : Collections.emptyList();

        if (leaders.size() > maxLeaders) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "领队人数超过上限 (" + maxLeaders + "人)");
        }
        if (1 + members.size() > maxMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队员人数超过上限 (" + maxMembers + "人，含队长)");
        }

        User captain = team.getCaptain();
        Map<String, User> verifiedLeaderUsers = validateAndResolveRegisteredUsers(competitionId, captain, leaders, "领队");
        Map<String, User> verifiedMemberUsers = validateAndResolveRegisteredUsers(competitionId, captain, members, "队员");

        // 清理当前队伍中未同意 (INVITED) 的旧成员并重新建立邀请
        List<CompetitionMember> currentMembers = memberRepository.findByTeamId(team.getId());
        Set<Long> newTargetUserIds = new HashSet<>();
        verifiedLeaderUsers.values().forEach(u -> newTargetUserIds.add(u.getId()));
        verifiedMemberUsers.values().forEach(u -> newTargetUserIds.add(u.getId()));

        for (CompetitionMember m : currentMembers) {
            if (m.getRole() != MemberRole.CAPTAIN && !newTargetUserIds.contains(m.getUser().getId())) {
                if (m.getStatus() == MemberStatus.INVITED) {
                    memberRepository.delete(m);
                }
            }
        }

        Instant expireTime = Instant.now().plus(24, ChronoUnit.HOURS);
        Set<Long> existingMemberUserIds = currentMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());

        for (TeamMemberInputDTO l : leaders) {
            User user = verifiedLeaderUsers.get(l.getPhone().trim());
            if (!existingMemberUserIds.contains(user.getId())) {
                saveInvitedMemberAndNotify(competition, team, captain, user, MemberRole.LEADER, l.getRemark(), expireTime);
            }
        }
        for (TeamMemberInputDTO m : members) {
            User user = verifiedMemberUsers.get(m.getPhone().trim());
            if (!existingMemberUserIds.contains(user.getId())) {
                saveInvitedMemberAndNotify(competition, team, captain, user, MemberRole.MEMBER, m.getRemark(), expireTime);
            }
        }

        // 核心降级逻辑：若已被审核通过，一旦修改队伍信息则自动降级为 AUDIT_DRAFT
        if (team.getStatus() == TeamStatus.AUDIT_PASSED) {
            team.setStatus(TeamStatus.AUDIT_DRAFT);
            log.info("[Competition-Team] 队伍 {} 因修改信息，状态由 AUDIT_PASSED 重置为 AUDIT_DRAFT", team.getTeamName());
        }

        teamRepository.save(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(Long competitionId, Long teamId, Long userId, TeamInviteAcceptDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        validateRegistrationPeriod(competition);

        CompetitionTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "队伍不存在"));

        CompetitionMember member = memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您未收到该队伍的邀请或邀请不存在"));

        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该邀请已处理或状态已变更: " + member.getStatus().getDescription());
        }

        // 严格冲突校验：不能在其他队伍处于 INVITED_ACCEPTED，且不能是其他队伍的队长
        Optional<CompetitionMember> otherAccepted = memberRepository.findByCompetitionIdAndUserId(competitionId, userId);
        if (otherAccepted.isPresent() && !Objects.equals(otherAccepted.get().getTeam().getId(), teamId)
                && otherAccepted.get().getStatus() == MemberStatus.INVITED_ACCEPTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "您已在本次竞赛的其他队伍中确认参赛，无法接受此邀请");
        }

        if (teamRepository.existsByCompetitionIdAndCaptain_Id(competitionId, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "您已是本次竞赛其他队伍的队长，无法接受此邀请");
        }

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        int maxMembers = config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4;
        long currentAccepted = memberRepository.countByTeamIdAndStatus(teamId, MemberStatus.INVITED_ACCEPTED);
        if (currentAccepted >= maxMembers) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "队伍人数已达上限 (" + maxMembers + "人)");
        }

        User user = member.getUser();
        if (request != null && request.getDynamicFormData() != null) {
            validateAndSaveRegistrationData(competition, team, user, request.getDynamicFormData(), member.getRole());
        }

        member.setStatus(MemberStatus.INVITED_ACCEPTED);
        memberRepository.save(member);

        eventPublisher.publishEvent(new TeamInvitationHandledEvent(
                this,
                competitionId,
                competition.getTitle(),
                team.getId(),
                team.getTeamName(),
                team.getCaptain().getId(),
                user.getId(),
                user.getUsername(),
                MemberStatus.INVITED_ACCEPTED
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
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您未收到该队伍的邀请"));

        if (member.getStatus() != MemberStatus.INVITED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该邀请已处理或状态已变更: " + member.getStatus().getDescription());
        }

        member.setStatus(MemberStatus.NOT_PARTICIPATE);
        memberRepository.save(member);

        eventPublisher.publishEvent(new TeamInvitationHandledEvent(
                this,
                competitionId,
                competition.getTitle(),
                team.getId(),
                team.getTeamName(),
                team.getCaptain().getId(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                MemberStatus.NOT_PARTICIPATE
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

        eventPublisher.publishEvent(new TeamInvitationHandledEvent(
                this,
                competitionId,
                team.getCompetition().getTitle(),
                team.getId(),
                team.getTeamName(),
                captainUserId,
                targetUserId,
                member.getUser().getUsername(),
                MemberStatus.NOT_PARTICIPATE
        ));
    }

    @Override
    public MyTeamInfoVO getMyTeam(Long competitionId, Long userId) {
        CompetitionMember member = memberRepository.findMemberWithTeam(competitionId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您尚未加入该竞赛的任何队伍"));

        CompetitionTeam team = member.getTeam();
        List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
        List<CompetitionMemberVO> memberVOs = competitionMapper.toMemberVOList(members);

        List<CompetitionRegistrationData> regData = registrationDataRepository.findByCompetitionIdAndUserId(competitionId, userId);
        Map<String, String> dataMap = regData.stream()
                .collect(Collectors.toMap(CompetitionRegistrationData::getFieldKey, CompetitionRegistrationData::getFieldValue, (k1, k2) -> k2));

        return MyTeamInfoVO.builder()
                .teamId(team.getId())
                .competitionId(competitionId)
                .competitionTitle(team.getCompetition() != null ? team.getCompetition().getTitle() : null)
                .teamName(team.getTeamName())
                .teamSlogan(team.getTeamSlogan())
                .avatarUrl(team.getAvatarUrl())
                .inviteCode(team.getInviteCode())
                .status(team.getStatus())
                .auditReason(team.getAuditReason())
                .myRole(member.getRole())
                .members(memberVOs)
                .myRegistrationData(dataMap)
                .createdAt(team.getCreatedAt())
                .build();
    }

    @Override
    public List<CompetitionTeamVO> listTeams(Long competitionId) {
        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED);
        List<CompetitionTeamVO> result = new ArrayList<>();
        for (CompetitionTeam team : teams) {
            CompetitionTeamVO vo = competitionMapper.toTeamVO(team);
            List<CompetitionMember> members = memberRepository.findByTeamId(team.getId());
            vo.setMembers(competitionMapper.toMemberVOList(members));
            result.add(vo);
        }
        return result;
    }

    private void validateRegistrationPeriod(Competition comp) {
        Instant now = Instant.now();
        if (comp.getRegStartTime() != null && now.isBefore(comp.getRegStartTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "报名尚未开启");
        }
        if (comp.getRegEndTime() != null && now.isAfter(comp.getRegEndTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "报名已截止");
        }
    }

    private Map<String, User> validateAndResolveRegisteredUsers(
            Long competitionId, User captain, List<TeamMemberInputDTO> memberInputs, String roleLabel) {
        Map<String, User> resultMap = new HashMap<>();
        Set<String> seenPhones = new HashSet<>();

        for (TeamMemberInputDTO input : memberInputs) {
            String phone = input.getPhone() != null ? input.getPhone().trim() : "";
            if (!StringUtils.hasText(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, roleLabel + "手机号不能为空");
            }

            if (captain.getPhone() != null && captain.getPhone().equals(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不能填写队长自身的手机号作为" + roleLabel);
            }

            if (!seenPhones.add(phone)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "填写的" + roleLabel + "手机号重复: " + phone);
            }

            // 1. 存活性验证
            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            roleLabel + "手机号 [" + phone + "] 尚未在平台注册，请先注册后再报名"));

            // 2. 冲突校验
            Optional<CompetitionMember> existingMemberOpt = memberRepository.findByCompetitionIdAndUserId(competitionId, user.getId());
            if (existingMemberOpt.isPresent() && existingMemberOpt.get().getStatus() == MemberStatus.INVITED_ACCEPTED) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        roleLabel + " [" + user.getUsername() + " (" + phone + ")] 已在其他队伍确认参赛，无法重复加入");
            }

            if (teamRepository.existsByCompetitionIdAndCaptain_Id(competitionId, user.getId())) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        roleLabel + " [" + user.getUsername() + " (" + phone + ")] 已是本次竞赛其他队伍的队长，无法重复加入");
            }

            resultMap.put(phone, user);
        }

        return resultMap;
    }

    private void saveInvitedMemberAndNotify(
            Competition competition, CompetitionTeam team, User captain, User invitee,
            MemberRole role, String remark, Instant expireTime) {
        CompetitionMember member = CompetitionMember.builder()
                .competition(competition)
                .team(team)
                .user(invitee)
                .role(role)
                .status(MemberStatus.INVITED)
                .build();
        memberRepository.save(member);

        eventPublisher.publishEvent(new TeamInvitationSentEvent(
                this,
                competition.getId(),
                competition.getTitle(),
                team.getId(),
                team.getTeamName(),
                captain.getId(),
                captain.getUsername(),
                invitee.getId(),
                role,
                remark,
                expireTime
        ));
    }

    private void validateAndSaveRegistrationData(Competition competition, CompetitionTeam team, User user, Map<String, String> data, MemberRole role) {
        List<CompetitionFieldSchema> schemas = fieldSchemaRepository.findByCompetitionIdOrderBySortOrderAsc(competition.getId());
        if (schemas.isEmpty()) {
            return;
        }

        Map<String, String> inputData = data != null ? data : Collections.emptyMap();
        List<CompetitionRegistrationData> recordsToSave = new ArrayList<>();

        for (CompetitionFieldSchema schema : schemas) {
            if (role != MemberRole.CAPTAIN && schema.getFillScope() == FillScope.CAPTAIN_ONLY) {
                continue;
            }

            String value = inputData.get(schema.getFieldKey());
            if (Boolean.TRUE.equals(schema.getIsRequired())) {
                if (!StringUtils.hasText(value)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "请填写: " + schema.getFieldLabel());
                }
            }

            if (StringUtils.hasText(value) && StringUtils.hasText(schema.getRegexPattern())) {
                try {
                    if (!Pattern.matches(schema.getRegexPattern(), value.trim())) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, schema.getFieldLabel() + " 格式不正确");
                    }
                } catch (Exception e) {
                    if (e instanceof BusinessException) throw e;
                }
            }

            recordsToSave.add(CompetitionRegistrationData.builder()
                    .competition(competition)
                    .team(team)
                    .user(user)
                    .fieldKey(schema.getFieldKey())
                    .fieldValue(value != null ? value.trim() : "")
                    .build());
        }

        registrationDataRepository.saveAll(recordsToSave);
    }

}
