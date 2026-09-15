package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.admin.impl.AdminTeamServiceImpl;
import com.humolix.volur.notification.domain.enums.NotificationType;
import com.humolix.volur.notification.event.SendNotificationEvent;
import com.humolix.volur.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTeamServiceImplTest {

    @Mock
    private CompetitionTeamRepository teamRepository;

    @Mock
    private CompetitionMemberRepository memberRepository;

    @Mock
    private CompetitionConfigRepository configRepository;

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminTeamServiceImpl adminTeamService;

    private final Long competitionId = 100L;
    private final Long teamId = 200L;
    private Competition competition;
    private CompetitionTeam team;
    private CompetitionMember captainMember;
    private CompetitionMember regularMember;

    @BeforeEach
    void setUp() {
        competition = Competition.builder()
                .id(competitionId)
                .title("2026全国网络安全攻防赛")
                .slug("security-cup-2026")
                .build();

        User captainUser = User.builder().id(1L).username("captainUser").build();
        User regularUser = User.builder().id(2L).username("regularUser").build();

        team = CompetitionTeam.builder()
                .id(teamId)
                .competition(competition)
                .teamName("战狼小队")
                .captain(captainUser)
                .status(TeamStatus.AUDIT_DRAFT)
                .build();

        captainMember = CompetitionMember.builder()
                .id(10L)
                .team(team)
                .user(captainUser)
                .role(MemberRole.CAPTAIN)
                .status(MemberStatus.INVITED_ACCEPTED)
                .build();

        regularMember = CompetitionMember.builder()
                .id(11L)
                .team(team)
                .user(regularUser)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.INVITED_ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("战队审核通过：状态更新为 AUDIT_PASSED 并向全队异步广播 SendNotificationEvent (COMPETITION_AUDIT)")
    void testAuditTeam_whenPassed_publishesNotificationEvents() {
        TeamAuditRequestDTO request = new TeamAuditRequestDTO();
        request.setStatus(TeamStatus.AUDIT_PASSED);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember, regularMember));
        when(configRepository.findByCompetitionId(competitionId)).thenReturn(Optional.empty());

        adminTeamService.auditTeam(teamId, request);

        assertEquals(TeamStatus.AUDIT_PASSED, team.getStatus());
        assertNull(team.getAuditReason());
        verify(teamRepository).save(team);

        ArgumentCaptor<SendNotificationEvent> captor = ArgumentCaptor.forClass(SendNotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<SendNotificationEvent> events = captor.getAllValues();
        assertEquals(2, events.size());
        for (SendNotificationEvent event : events) {
            assertEquals(NotificationType.COMPETITION_AUDIT, event.getType());
            assertTrue(event.getTitle().contains("已通过审核"));
            assertTrue(event.getContent().contains("战狼小队"));
            assertNull(event.getSenderId()); // 系统发送
        }
    }

    @Test
    @DisplayName("战队审核驳回：状态更新为 AUDIT_REJECTED 并向全队异步广播 SendNotificationEvent (COMPETITION_AUDIT)")
    void testAuditTeam_whenRejected_publishesNotificationEvents() {
        TeamAuditRequestDTO request = new TeamAuditRequestDTO();
        request.setStatus(TeamStatus.AUDIT_REJECTED);
        request.setAuditReason("队伍名称不符合规范，请重新修改");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember, regularMember));

        adminTeamService.auditTeam(teamId, request);

        assertEquals(TeamStatus.AUDIT_REJECTED, team.getStatus());
        assertEquals("队伍名称不符合规范，请重新修改", team.getAuditReason());
        verify(teamRepository).save(team);

        ArgumentCaptor<SendNotificationEvent> captor = ArgumentCaptor.forClass(SendNotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<SendNotificationEvent> events = captor.getAllValues();
        assertEquals(2, events.size());
        for (SendNotificationEvent event : events) {
            assertEquals(NotificationType.COMPETITION_AUDIT, event.getType());
            assertTrue(event.getTitle().contains("审核被驳回"));
            assertTrue(event.getContent().contains("队伍名称不符合规范"));
            assertNull(event.getSenderId()); // 系统发送
        }
    }

}
