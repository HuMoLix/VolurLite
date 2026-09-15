package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionMemberVO;
import com.humolix.volur.competition.domain.vo.CompetitionTeamVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.impl.CompetitionTeamServiceImpl;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.notification.domain.enums.NotificationActionStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
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
    private CompetitionMapper competitionMapper;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.humolix.volur.notification.service.NotificationService notificationService;

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

        User captainUser = User.builder().id(captainUserId).username("captain").build();
        User regularUser = User.builder().id(memberUserId).username("member").build();

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

        when(competitionRepository.existsById(competitionId)).thenReturn(true);
        when(teamRepository.findByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED))
                .thenReturn(List.of(team));
        when(memberRepository.findByTeamId(teamId))
                .thenReturn(List.of(captainMember, regularMember));

        when(competitionMapper.toTeamVO(team)).thenAnswer(inv -> CompetitionTeamVO.builder()
                .id(teamId)
                .teamName("Alpha")
                .captainId(captainUserId)
                .build());

        when(competitionMapper.toMemberVO(captainMember)).thenAnswer(inv -> CompetitionMemberVO.builder()
                .id(10L)
                .userId(captainUserId)
                .username("captain")
                .role(MemberRole.CAPTAIN)
                .realName("张三")
                .idCard("110101199001011234")
                .phone("13812345678")
                .email("zhangsan@example.com")
                .build());

        when(competitionMapper.toMemberVO(regularMember)).thenAnswer(inv -> CompetitionMemberVO.builder()
                .id(11L)
                .userId(memberUserId)
                .username("member")
                .role(MemberRole.MEMBER)
                .realName("李四")
                .idCard("110101199505054321")
                .phone("13987654321")
                .email("lisi@example.com")
                .build());
    }

    @Test
    @DisplayName("队长查看队伍列表：本队成员敏感信息完全不脱敏")
    void testListTeams_whenCaptain_shouldSeeUndesensitizedMemberInfo() {
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, captainUserId))
                .thenReturn(List.of(captainMember));

        List<CompetitionTeamVO> result = teamService.listTeams(competitionId, captainUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        List<CompetitionMemberVO> members = result.get(0).getMembers();
        assertEquals(2, members.size());

        // 队长查看普通队员：明文
        CompetitionMemberVO regularVO = members.get(1);
        assertEquals("李四", regularVO.getRealName());
        assertEquals("110101199505054321", regularVO.getIdCard());
        assertEquals("13987654321", regularVO.getPhone());
        assertEquals("lisi@example.com", regularVO.getEmail());
    }

    @Test
    @DisplayName("队员之间查看队伍列表：队友敏感信息进行脱敏，自身信息保持完整")
    void testListTeams_whenMember_shouldDesensitizeTeammatesKeyInfo() {
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, memberUserId))
                .thenReturn(List.of(regularMember));

        List<CompetitionTeamVO> result = teamService.listTeams(competitionId, memberUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        List<CompetitionMemberVO> members = result.get(0).getMembers();
        assertEquals(2, members.size());

        // 队员查看队长（队友）：关键信息脱敏（idCard, phone, email），realName 不脱敏
        CompetitionMemberVO captainVO = members.get(0);
        assertEquals("张三", captainVO.getRealName());
        assertEquals("110101********1234", captainVO.getIdCard());
        assertEquals("138****5678", captainVO.getPhone());
        assertEquals("z***n@example.com", captainVO.getEmail());

        // 队员查看自己：明文
        CompetitionMemberVO selfVO = members.get(1);
        assertEquals("李四", selfVO.getRealName());
        assertEquals("110101199505054321", selfVO.getIdCard());
        assertEquals("13987654321", selfVO.getPhone());
        assertEquals("lisi@example.com", selfVO.getEmail());
    }

    @Test
    @DisplayName("外部人员查看队伍列表：非本队队员敏感信息全部隐藏（置为 null）")
    void testListTeams_whenOutsider_shouldHidePrivateInfo() {
        when(memberRepository.findAllByCompetitionIdAndUserId(competitionId, outsiderUserId))
                .thenReturn(List.of());

        List<CompetitionTeamVO> result = teamService.listTeams(competitionId, outsiderUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        List<CompetitionMemberVO> members = result.get(0).getMembers();
        assertEquals(2, members.size());

        // 外部人员查看队伍成员：敏感信息全部为 null，保留 username / role 等公共属性
        for (CompetitionMemberVO vo : members) {
            assertNull(vo.getIdCard());
            assertNull(vo.getPhone());
            assertNull(vo.getEmail());
            assertNull(vo.getRealName());
            assertNotNull(vo.getUsername());
            assertNotNull(vo.getRole());
        }
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

        verify(memberRepository).deleteByTeamId(teamId);
        verify(teamRepository).delete(team);
    }

}
