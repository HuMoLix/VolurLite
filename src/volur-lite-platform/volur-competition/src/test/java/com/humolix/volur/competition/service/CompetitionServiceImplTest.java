package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionAnnouncement;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionAnnouncementRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.impl.CompetitionServiceImpl;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceImplTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionTeamRepository teamRepository;

    @Mock
    private CompetitionMemberRepository memberRepository;

    @Mock
    private CompetitionAnnouncementRepository announcementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private VolurCacheTemplate cacheTemplate;

    @InjectMocks
    private CompetitionServiceImpl competitionService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(cacheTemplate.getOrLoad(anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    Callable<?> loader = invocation.getArgument(2);
                    return loader.call();
                });
    }

    @Test
    @DisplayName("竞赛不存在时获取公告应抛出 404 异常")
    void testListAnnouncements_Throws404WhenCompetitionNotFound() {
        when(competitionRepository.existsById(1L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> competitionService.listAnnouncements(1L, 100L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("竞赛不存在", ex.getMessage());
    }

    @Test
    @DisplayName("用户未登录获取公告应抛出 401 异常")
    void testListAnnouncements_Throws401WhenUserNotLoggedIn() {
        when(competitionRepository.existsById(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> competitionService.listAnnouncements(1L, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("用户未登录", ex.getMessage());
    }

    @Test
    @DisplayName("用户未加入已审核通过的参赛队伍且非管理员时应抛出 403 异常")
    void testListAnnouncements_Throws403WhenNotApprovedTeamOrAdmin() {
        when(competitionRepository.existsById(1L)).thenReturn(true);
        when(memberRepository.findMemberWithTeam(1L, 100L)).thenReturn(Optional.empty());

        User regularUser = User.builder()
                .id(100L)
                .role(UserRole.USER)
                .build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(regularUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> competitionService.listAnnouncements(1L, 100L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("您尚未加入已审核通过的参赛队伍", ex.getMessage());
    }

    @Test
    @DisplayName("审核通过的参赛队员获取公告应成功")
    void testListAnnouncements_SuccessWhenApprovedTeamMember() {
        when(competitionRepository.existsById(1L)).thenReturn(true);

        CompetitionTeam approvedTeam = CompetitionTeam.builder()
                .id(20L)
                .status(TeamStatus.AUDIT_PASSED)
                .build();
        CompetitionMember member = CompetitionMember.builder()
                .id(50L)
                .status(MemberStatus.INVITED_ACCEPTED)
                .team(approvedTeam)
                .build();
        when(memberRepository.findMemberWithTeam(1L, 100L)).thenReturn(Optional.of(member));

        CompetitionAnnouncement announcement = CompetitionAnnouncement.builder()
                .id(10L)
                .title("比赛通知")
                .build();
        when(announcementRepository.findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(1L))
                .thenReturn(List.of(announcement));

        CompetitionAnnouncementVO vo = CompetitionAnnouncementVO.builder()
                .id(10L)
                .title("比赛通知")
                .build();
        when(competitionMapper.toAnnouncementVOList(anyList())).thenReturn(List.of(vo));

        List<CompetitionAnnouncementVO> result = competitionService.listAnnouncements(1L, 100L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("比赛通知", result.get(0).getTitle());
    }

    @Test
    @DisplayName("平台管理员获取公告应成功无需加入战队")
    void testListAnnouncements_SuccessWhenAdmin() {
        when(competitionRepository.existsById(1L)).thenReturn(true);
        when(memberRepository.findMemberWithTeam(1L, 100L)).thenReturn(Optional.empty());

        User adminUser = User.builder()
                .id(100L)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(adminUser));

        CompetitionAnnouncement announcement = CompetitionAnnouncement.builder()
                .id(10L)
                .title("管理员巡检通知")
                .build();
        when(announcementRepository.findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(1L))
                .thenReturn(List.of(announcement));

        CompetitionAnnouncementVO vo = CompetitionAnnouncementVO.builder()
                .id(10L)
                .title("管理员巡检通知")
                .build();
        when(competitionMapper.toAnnouncementVOList(anyList())).thenReturn(List.of(vo));

        List<CompetitionAnnouncementVO> result = competitionService.listAnnouncements(1L, 100L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("管理员巡检通知", result.get(0).getTitle());
    }

}
