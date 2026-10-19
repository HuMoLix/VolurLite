package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.AdminTeamUpdateDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamAuditDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamDeleteDTO;
import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.OrganizationType;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.*;
import com.humolix.volur.competition.repository.awdp.AwdPlusCompetitionPatchRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.service.admin.impl.AdminTeamServiceImpl;
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
    private CompetitionChatMessageRepository chatMessageRepository;

    @Mock
    private CompetitionTeamInvitationRepository teamInvitationRepository;

    @Mock
    private CtfCompetitionSubmissionRepository ctfSubmissionRepository;

    @Mock
    private TheoryCompetitionAnswerRepository theoryAnswerRepository;

    @Mock
    private AwdPlusCompetitionPatchRepository awdPlusPatchRepository;

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

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

    @Test
    @DisplayName("删除队伍：正常流程级联清理邀请记录、聊天记录、队员关系并删除队伍主体")
    void testDeleteTeam_success_cascadesDeletions() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(ctfSubmissionRepository.existsByTeamId(teamId)).thenReturn(false);
        when(theoryAnswerRepository.existsByTeamId(teamId)).thenReturn(false);
        when(awdPlusPatchRepository.existsByTeamId(teamId)).thenReturn(false);

        adminTeamService.deleteTeam(teamId);

        verify(teamInvitationRepository).deleteByTeamId(teamId);
        verify(chatMessageRepository).deleteByTeamId(teamId);
        verify(memberRepository).deleteByTeamId(teamId);
        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("删除队伍：若战队已存在作答提交记录，抛出 BusinessException 拒绝删除")
    void testDeleteTeam_whenHasSubmissions_throwsBusinessException() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(ctfSubmissionRepository.existsByTeamId(teamId)).thenReturn(true);

        com.humolix.volur.framework.common.exception.BusinessException ex =
                assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class,
                        () -> adminTeamService.deleteTeam(teamId));

        assertTrue(ex.getMessage().contains("已有赛题作答或提交记录"));
        verify(teamInvitationRepository, never()).deleteByTeamId(any());
        verify(chatMessageRepository, never()).deleteByTeamId(any());
        verify(memberRepository, never()).deleteByTeamId(any());
        verify(teamRepository, never()).delete(any(CompetitionTeam.class));
    }

    @Test
    @DisplayName("删除队伍：队伍不存在时抛出 404 BusinessException")
    void testDeleteTeam_whenNotFound_throwsBusinessException() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class,
                () -> adminTeamService.deleteTeam(teamId));
    }

    @Test
    @DisplayName("修改队伍：成功修改队伍名称、描述、所属机构及机构类型")
    void testUpdateTeam_success() {
        AdminTeamUpdateDTO dto = new AdminTeamUpdateDTO();
        dto.setTeamName("苍鹰突击队");
        dto.setDescription("新口号");
        dto.setOrganization("清华大学");
        dto.setOrganizationType(OrganizationType.SCHOOL);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByCompetitionIdAndTeamNameAndIdNot(competitionId, "苍鹰突击队", teamId)).thenReturn(false);

        adminTeamService.updateTeam(teamId, dto);

        assertEquals("苍鹰突击队", team.getTeamName());
        assertEquals("新口号", team.getDescription());
        assertEquals("清华大学", team.getOrganization());
        assertEquals(OrganizationType.SCHOOL, team.getOrganizationType());
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("修改队伍：更新成员全量信息并修改指定队长")
    void testUpdateTeam_withMembersAndCaptainChange() {
        AdminTeamUpdateDTO dto = new AdminTeamUpdateDTO();
        dto.setTeamName("战狼小队");

        // 将原队员 regularMember 设为队长，原队长 captainMember 设为队员
        com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO m1 = new com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO();
        m1.setId(regularMember.getId());
        m1.setRole(MemberRole.CAPTAIN);
        m1.setRealName("新队长张三");
        m1.setPhone("13900001111");

        com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO m2 = new com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO();
        m2.setId(captainMember.getId());
        m2.setRole(MemberRole.MEMBER);
        m2.setRealName("原队长李四");

        dto.setMembers(List.of(m1, m2));

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByCompetitionIdAndTeamNameAndIdNot(competitionId, "战狼小队", teamId)).thenReturn(false);
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember, regularMember));

        adminTeamService.updateTeam(teamId, dto);

        assertEquals(MemberRole.CAPTAIN, regularMember.getRole());
        assertEquals("新队长张三", regularMember.getRealName());
        assertEquals("13900001111", regularMember.getPhone());
        assertEquals(MemberRole.MEMBER, captainMember.getRole());
        assertEquals("原队长李四", captainMember.getRealName());
        assertEquals(regularMember.getUser(), team.getCaptain());
        verify(memberRepository).save(regularMember);
        verify(memberRepository).save(captainMember);
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("修改队伍：新队名与同比赛下其他队伍重名时抛出 BusinessException")
    void testUpdateTeam_duplicateName_throwsBusinessException() {
        AdminTeamUpdateDTO dto = new AdminTeamUpdateDTO();
        dto.setTeamName("已存在的战队");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByCompetitionIdAndTeamNameAndIdNot(competitionId, "已存在的战队", teamId)).thenReturn(true);

        com.humolix.volur.framework.common.exception.BusinessException ex =
                assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class,
                        () -> adminTeamService.updateTeam(teamId, dto));

        assertTrue(ex.getMessage().contains("已存在"));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改队伍：成员列表中无队长或包含多个队长时抛出 BusinessException")
    void testUpdateTeam_whenCaptainCountNotOne_throwsBusinessException() {
        AdminTeamUpdateDTO dto = new AdminTeamUpdateDTO();
        dto.setTeamName("新战队");

        com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO m1 = new com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO();
        m1.setId(10L);
        m1.setRole(MemberRole.MEMBER); // 0 个队长

        dto.setMembers(List.of(m1));

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByCompetitionIdAndTeamNameAndIdNot(competitionId, "新战队", teamId)).thenReturn(false);
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember));

        com.humolix.volur.framework.common.exception.BusinessException ex1 =
                assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class,
                        () -> adminTeamService.updateTeam(teamId, dto));
        assertTrue(ex1.getMessage().contains("必须且只能包含 1 名队长"));

        // 2 个队长测试
        com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO m2 = new com.humolix.volur.competition.domain.dto.AdminTeamMemberDTO();
        m2.setId(11L);
        m2.setRole(MemberRole.CAPTAIN);
        m1.setRole(MemberRole.CAPTAIN);
        dto.setMembers(List.of(m1, m2));

        com.humolix.volur.framework.common.exception.BusinessException ex2 =
                assertThrows(com.humolix.volur.framework.common.exception.BusinessException.class,
                        () -> adminTeamService.updateTeam(teamId, dto));
        assertTrue(ex2.getMessage().contains("必须且只能包含 1 名队长"));
    }

    @Test
    @DisplayName("获取队伍统计：各状态统计数精确计算并返回 AdminTeamStatsVO")
    void testGetTeamStats_success() {
        when(teamRepository.countByCompetitionId(competitionId)).thenReturn(10L);
        when(teamRepository.countByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_DRAFT)).thenReturn(3L);
        when(teamRepository.countByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_PASSED)).thenReturn(5L);
        when(teamRepository.countByCompetitionIdAndStatus(competitionId, TeamStatus.AUDIT_REJECTED)).thenReturn(2L);

        com.humolix.volur.competition.domain.vo.AdminTeamStatsVO stats = adminTeamService.getTeamStats(competitionId);

        assertEquals(10L, stats.getTotal());
        assertEquals(3L, stats.getPending());
        assertEquals(5L, stats.getApproved());
        assertEquals(2L, stats.getRejected());
    }

    @Test
    @DisplayName("批量审核队伍：成功批量审核通过并广播通知")
    void testBatchAuditTeams() {
        BatchTeamAuditDTO dto = new BatchTeamAuditDTO();
        dto.setTeamIds(List.of(teamId));
        dto.setStatus(TeamStatus.AUDIT_PASSED);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamId(teamId)).thenReturn(List.of(captainMember));

        adminTeamService.batchAuditTeams(competitionId, dto);

        assertEquals(TeamStatus.AUDIT_PASSED, team.getStatus());
        verify(teamRepository).save(team);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(SendNotificationEvent.class));
    }

    @Test
    @DisplayName("批量删除队伍：成功批量级联删除队伍")
    void testBatchDeleteTeams() {
        BatchTeamDeleteDTO dto = new BatchTeamDeleteDTO();
        dto.setTeamIds(List.of(teamId));

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(ctfSubmissionRepository.existsByTeamId(teamId)).thenReturn(false);
        when(theoryAnswerRepository.existsByTeamId(teamId)).thenReturn(false);
        when(awdPlusPatchRepository.existsByTeamId(teamId)).thenReturn(false);

        adminTeamService.batchDeleteTeams(competitionId, dto.getTeamIds());

        verify(teamInvitationRepository).deleteByTeamId(teamId);
        verify(chatMessageRepository).deleteByTeamId(teamId);
        verify(memberRepository).deleteByTeamId(teamId);
        verify(teamRepository).delete(team);
    }
}
