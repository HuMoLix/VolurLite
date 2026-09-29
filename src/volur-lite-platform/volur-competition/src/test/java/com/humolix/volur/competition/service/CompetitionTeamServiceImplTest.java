package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamMemberSignupDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.*;
import com.humolix.volur.competition.service.impl.CompetitionTeamServiceImpl;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompetitionTeamServiceImplTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private com.humolix.volur.competition.repository.CompetitionConfigRepository configRepository;

    @Mock
    private CompetitionTeamRepository teamRepository;

    @Mock
    private CompetitionMemberRepository memberRepository;

    @Mock
    private CompetitionChatMessageRepository chatMessageRepository;

    @Mock
    private CompetitionTeamInvitationRepository teamInvitationRepository;

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.humolix.volur.notification.service.NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompetitionTeamServiceImpl teamService;

    private final Long competitionId = 100L;
    private final Long teamId = 200L;
    private final Long captainUserId = 1L;
    private final Long memberUserId = 2L;
    private final Long outsiderUserId = 999L;

    private CompetitionTeam team;
    private CompetitionMember captainMember;
    private CompetitionMember regularMember;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        User captainUser = User.builder().id(captainUserId).username("captain").email("captain@example.com").build();
        User regularUser = User.builder().id(memberUserId).username("member").email("member@example.com").build();

        team = CompetitionTeam.builder()
                .id(teamId)
                .teamName("Alpha")
                .captain(captainUser)
                .status(TeamStatus.AUDIT_PASSED)
                .build();

        captainMember = CompetitionMember.builder()
                .id(10L)
                .team(team)
                .user(captainUser)
                .role(MemberRole.CAPTAIN)
                .status(MemberStatus.INVITED_ACCEPTED)
                .realName("张三")
                .idCard("110101199001011234")
                .phone("13812345678")
                .email("zhangsan@example.com")
                .build();

        regularMember = CompetitionMember.builder()
                .id(11L)
                .team(team)
                .user(regularUser)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.INVITED_ACCEPTED)
                .realName("李四")
                .idCard("110101199505054321")
                .phone("13987654321")
                .email("lisi@example.com")
                .build();
    }

    @Test
    @DisplayName("接受邀请：更新邀请卡片状态为 ACCEPTED 并向队长发布 SendNotificationEvent 反馈")
    void testAcceptInvitation_updatesActionStatusAndPublishesFeedback() {
        Competition competition = Competition.builder().id(competitionId).title("测试网络安全竞赛").build();
        CompetitionMember invitedMember = CompetitionMember.builder()
                .id(99L)
                .competition(competition)
                .team(team)
                .user(User.builder().id(memberUserId).username("member").build())
                .role(MemberRole.MEMBER)
                .status(MemberStatus.INVITED)
                .build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(teamRepository.findById(teamId)).thenReturn(java.util.Optional.of(team));
        when(memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, memberUserId))
                .thenReturn(java.util.Optional.of(invitedMember));
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, memberUserId))
                .thenReturn(List.of());
        when(teamRepository.existsByCompetitionIdAndCaptain_Id(competitionId, memberUserId)).thenReturn(false);
        when(configRepository.findByCompetitionId(competitionId)).thenReturn(java.util.Optional.empty());
        when(memberRepository.findByTeamIdAndStatus(teamId, MemberStatus.INVITED_ACCEPTED)).thenReturn(List.of(captainMember));

        teamService.acceptInvitation(competitionId, teamId, memberUserId, new TeamInviteAcceptDTO());

        assertEquals(MemberStatus.INVITED_ACCEPTED, invitedMember.getStatus());
        verify(memberRepository).save(invitedMember);

        verify(notificationService).updateActionStatus(
                eq(memberUserId),
                eq(NotificationType.COMPETITION_INVITE),
                eq("\"teamId\":" + teamId),
                eq(NotificationActionStatus.ACCEPTED)
        );

        ArgumentCaptor<SendNotificationEvent> eventCaptor = ArgumentCaptor.forClass(SendNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SendNotificationEvent event = eventCaptor.getValue();
        assertEquals(memberUserId, event.getSenderId());
        assertEquals(captainUserId, event.getReceiverId());
        assertEquals(NotificationType.COMPETITION_INVITE, event.getType());
        assertTrue(event.getTitle().contains("已同意加入战队"));
    }

    @Test
    @DisplayName("拒绝邀请：更新邀请卡片状态为 REJECTED 并向队长发布 SendNotificationEvent 反馈")
    void testRejectInvitation_updatesActionStatusAndPublishesFeedback() {
        Competition competition = Competition.builder().id(competitionId).title("测试网络安全竞赛").build();
        CompetitionMember invitedMember = CompetitionMember.builder()
                .id(99L)
                .competition(competition)
                .team(team)
                .user(User.builder().id(memberUserId).username("member").build())
                .role(MemberRole.MEMBER)
                .status(MemberStatus.INVITED)
                .build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(teamRepository.findById(teamId)).thenReturn(java.util.Optional.of(team));
        when(memberRepository.findByCompetitionIdAndTeamIdAndUserId(competitionId, teamId, memberUserId))
                .thenReturn(java.util.Optional.of(invitedMember));

        teamService.rejectInvitation(competitionId, teamId, memberUserId);

        assertEquals(MemberStatus.NOT_PARTICIPATE, invitedMember.getStatus());
        verify(memberRepository).save(invitedMember);

        verify(notificationService).updateActionStatus(
                eq(memberUserId),
                eq(NotificationType.COMPETITION_INVITE),
                eq("\"teamId\":" + teamId),
                eq(NotificationActionStatus.REJECTED)
        );

        ArgumentCaptor<SendNotificationEvent> eventCaptor = ArgumentCaptor.forClass(SendNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SendNotificationEvent event = eventCaptor.getValue();
        assertEquals(memberUserId, event.getSenderId());
        assertEquals(captainUserId, event.getReceiverId());
        assertEquals(NotificationType.COMPETITION_INVITE, event.getType());
        assertTrue(event.getTitle().contains("拒绝了入队邀请"));
    }

    @Test
    @DisplayName("解散队伍：取消队员邀请卡片并向所有队员发布解散通知 SendNotificationEvent")
    void testDeleteMyTeam_cancelsActionStatusAndNotifiesMembers() {
        Competition competition = Competition.builder().id(competitionId).title("测试网络安全竞赛").build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(teamRepository.findByCompetitionIdAndCaptain_Id(competitionId, captainUserId)).thenReturn(java.util.Optional.of(team));
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember, regularMember));

        teamService.deleteMyTeam(competitionId, captainUserId);

        verify(notificationService).updateActionStatus(
                eq(memberUserId),
                eq(NotificationType.COMPETITION_INVITE),
                eq("\"teamId\":" + teamId),
                eq(NotificationActionStatus.CANCELLED)
        );

        ArgumentCaptor<SendNotificationEvent> eventCaptor = ArgumentCaptor.forClass(SendNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SendNotificationEvent event = eventCaptor.getValue();
        assertEquals(captainUserId, event.getSenderId());
        assertEquals(memberUserId, event.getReceiverId());
        assertEquals(NotificationType.COMPETITION_INVITE, event.getType());
        assertTrue(event.getTitle().contains("已被解散"));

        verify(teamInvitationRepository).deleteByTeamId(teamId);
        verify(chatMessageRepository).deleteByTeamId(teamId);
        verify(memberRepository).deleteByTeamId(teamId);
        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("战队报名：手机号已被本场竞赛其他队伍登记时抛出 409 CONFLICT")
    void testSignupTeam_whenPhoneAlreadyRegistered_shouldThrowConflict() {
        Competition competition = Competition.builder().id(competitionId).title("测试竞赛").build();
        User captain = User.builder().id(captainUserId).username("captain").email("captain@example.com").build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, captainUserId)).thenReturn(List.of());
        when(teamRepository.existsByCompetitionIdAndTeamName(competitionId, "BetaTeam")).thenReturn(false);
        when(configRepository.findByCompetitionId(competitionId)).thenReturn(java.util.Optional.empty());
        when(userRepository.findById(captainUserId)).thenReturn(java.util.Optional.of(captain));

        TeamMemberSignupDTO captainRow = new TeamMemberSignupDTO();
        captainRow.setRealName("队长张");
        captainRow.setGender(com.humolix.volur.user.domain.enums.Gender.MALE);
        captainRow.setRole(MemberRole.CAPTAIN);
        captainRow.setEmployeeNumber("2021001");
        captainRow.setPhone("15906469630");
        captainRow.setEmail("captain@example.com");
        captainRow.setIdCard("110101199001011234");
        captainRow.setDepartment("计算机学院");
        captainRow.setPosition("计科1班");

        TeamSignupRequestDTO request = new TeamSignupRequestDTO();
        request.setTeamName("BetaTeam");
        request.setMembers(List.of(captainRow));

        CompetitionMember conflictMember = CompetitionMember.builder()
                .id(999L)
                .phone("15906469630")
                .idCard("310101199505051111")
                .email("other@example.com")
                .role(MemberRole.MEMBER)
                .build();

        when(memberRepository.findConflictingMembers(eq(competitionId), anyCollection(), isNull(), anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of(conflictMember));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                teamService.signupTeam(competitionId, captainUserId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("15906469630"));
        assertTrue(ex.getMessage().contains("手机号"));
    }

    @Test
    @DisplayName("战队报名：身份证号已被本场竞赛其他队伍登记时抛出 409 CONFLICT")
    void testSignupTeam_whenIdCardAlreadyRegistered_shouldThrowConflict() {
        Competition competition = Competition.builder().id(competitionId).title("测试竞赛").build();
        User captain = User.builder().id(captainUserId).username("captain").email("captain@example.com").build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, captainUserId)).thenReturn(List.of());
        when(teamRepository.existsByCompetitionIdAndTeamName(competitionId, "BetaTeam")).thenReturn(false);
        when(configRepository.findByCompetitionId(competitionId)).thenReturn(java.util.Optional.empty());
        when(userRepository.findById(captainUserId)).thenReturn(java.util.Optional.of(captain));

        TeamMemberSignupDTO captainRow = new TeamMemberSignupDTO();
        captainRow.setRealName("队长张");
        captainRow.setGender(com.humolix.volur.user.domain.enums.Gender.MALE);
        captainRow.setRole(MemberRole.CAPTAIN);
        captainRow.setEmployeeNumber("2021001");
        captainRow.setPhone("13800001111");
        captainRow.setEmail("captain@example.com");
        captainRow.setIdCard("110101199001011234");
        captainRow.setDepartment("计算机学院");
        captainRow.setPosition("计科1班");

        TeamSignupRequestDTO request = new TeamSignupRequestDTO();
        request.setTeamName("BetaTeam");
        request.setMembers(List.of(captainRow));

        CompetitionMember conflictMember = CompetitionMember.builder()
                .id(999L)
                .phone("13999998888")
                .idCard("110101199001011234")
                .email("other@example.com")
                .role(MemberRole.MEMBER)
                .build();

        when(memberRepository.findConflictingMembers(eq(competitionId), anyCollection(), isNull(), anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of(conflictMember));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                teamService.signupTeam(competitionId, captainUserId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("身份证号"));
    }

    @Test
    @DisplayName("战队修改：修改队员名册时手机号与其他队伍冲突抛出 409 CONFLICT")
    void testUpdateTeam_whenPhoneConflictsWithOtherTeam_shouldThrowConflict() {
        Competition competition = Competition.builder().id(competitionId).title("测试竞赛").build();

        when(competitionRepository.findById(competitionId)).thenReturn(java.util.Optional.of(competition));
        when(teamRepository.findByCompetitionIdAndCaptain_Id(competitionId, captainUserId)).thenReturn(java.util.Optional.of(team));
        when(configRepository.findByCompetitionId(competitionId)).thenReturn(java.util.Optional.empty());

        TeamMemberSignupDTO captainRow = new TeamMemberSignupDTO();
        captainRow.setRealName("队长");
        captainRow.setGender(com.humolix.volur.user.domain.enums.Gender.MALE);
        captainRow.setRole(MemberRole.CAPTAIN);
        captainRow.setEmployeeNumber("2021001");
        captainRow.setPhone("13812345678");
        captainRow.setEmail("captain@example.com");
        captainRow.setIdCard("110101199001011234");
        captainRow.setDepartment("网安学院");
        captainRow.setPosition("网安1班");

        TeamMemberSignupDTO newMemberRow = new TeamMemberSignupDTO();
        newMemberRow.setRealName("新队员");
        newMemberRow.setGender(com.humolix.volur.user.domain.enums.Gender.MALE);
        newMemberRow.setRole(MemberRole.MEMBER);
        newMemberRow.setEmployeeNumber("2021002");
        newMemberRow.setPhone("15906469630");
        newMemberRow.setEmail("newmem@example.com");
        newMemberRow.setIdCard("440101199801012345");
        newMemberRow.setDepartment("网安学院");
        newMemberRow.setPosition("网安2班");

        TeamUpdateRequestDTO request = new TeamUpdateRequestDTO();
        request.setTeamName("Alpha");
        request.setMembers(List.of(captainRow, newMemberRow));

        CompetitionTeam otherTeam = CompetitionTeam.builder().id(999L).teamName("OtherTeam").build();
        CompetitionMember conflictMember = CompetitionMember.builder()
                .id(888L)
                .team(otherTeam)
                .phone("15906469630")
                .idCard("510101199701014321")
                .email("someone@example.com")
                .role(MemberRole.MEMBER)
                .build();

        when(memberRepository.findConflictingMembers(eq(competitionId), anyCollection(), eq(teamId), anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of(conflictMember));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                teamService.updateTeam(competitionId, captainUserId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("15906469630"));
    }

}
