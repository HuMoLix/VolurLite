package com.humolix.volur.competition.service;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.competition.domain.dto.ctf.CtfFlagSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionChallengeRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository;
import com.humolix.volur.competition.service.ctf.impl.CtfCompetitionServiceImpl;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.CachedValue;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.cache.redis.RedisOps;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.notification.service.RealtimePushService;
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CtfCompetitionCacheTest {

    @Mock
    private CompetitionMemberRepository memberRepository;
    @Mock
    private CtfCompetitionChallengeRepository challengeRepository;
    @Mock
    private CtfCompetitionSolvedRepository solvedRepository;
    @Mock
    private CtfCompetitionSubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CtfInstanceService containerInstanceService;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private VolurCacheTemplate cacheTemplate;
    @Mock
    private RedisOps redisOps;
    @Mock
    private RealtimePushService realtimePushService;

    @InjectMocks
    private CtfCompetitionServiceImpl ctfCompetitionService;

    private Competition competition;
    private CtfCompetitionChallenge compChallenge;
    private CompetitionTeam team;
    private CompetitionMember member;

    @BeforeEach
    void setUp() {
        competition = Competition.builder()
                .id(100L)
                .title("2026 迎新赛")
                .status(CompetitionStatus.RUNNING)
                .build();

        CtfChallenge rawChallenge = CtfChallenge.builder()
                .id(1L)
                .flag("flag{test_success}")
                .type(ChallengeType.STATIC)
                .build();

        compChallenge = CtfCompetitionChallenge.builder()
                .id(10L)
                .competition(competition)
                .challenge(rawChallenge)
                .initialPoints(1000)
                .currentPoints(1000)
                .solvesCount(0)
                .isVisible(true)
                .build();

        team = CompetitionTeam.builder()
                .id(200L)
                .competition(competition)
                .teamName("测试战队")
                .status(TeamStatus.AUDIT_PASSED)
                .build();

        User mockUser = User.builder().id(1L).username("testuser").build();

        member = CompetitionMember.builder()
                .id(300L)
                .competition(competition)
                .team(team)
                .user(mockUser)
                .status(MemberStatus.INVITED_ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("验证 listChallenges 动静分离：底座走缓存且动态标记解题状态不污染原缓存对象")
    @SuppressWarnings("unchecked")
    void testListChallengesDynamicSeparation() throws Exception {
        CtfCompetitionChallengeVO cachedVo = CtfCompetitionChallengeVO.builder()
                .id(10L)
                .name("签到题")
                .currentPoints(1000)
                .isSolvedByMyTeam(false)
                .build();

        List<CtfCompetitionChallengeVO> baseList = List.of(cachedVo);

        // 模拟 cacheTemplate.getOrLoad 返回底座
        when(cacheTemplate.getOrLoad(eq(CacheNames.COMPETITION_STATUS), eq("ctf:challenges:100"), any()))
                .thenReturn(baseList);

        // 模拟用户在队伍中，且队伍已解出题 10L
        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        CtfCompetitionSolved solved = CtfCompetitionSolved.builder()
                .competition(competition)
                .competitionChallenge(compChallenge)
                .team(team)
                .pointsAwarded(1000)
                .build();
        when(solvedRepository.findByCompetitionIdAndTeamId(100L, 200L)).thenReturn(List.of(solved));

        List<CtfCompetitionChallengeVO> result = ctfCompetitionService.listChallenges(100L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsSolvedByMyTeam(), "当前队伍解出了此题，派生副本中 isSolvedByMyTeam 必须为 true");
        assertFalse(cachedVo.getIsSolvedByMyTeam(), "原缓存中的对象状态必须保持不变，严禁被线程安全污染");
    }

    @Test
    @DisplayName("验证 CachedValue 经序列化与反序列化后 listChallenges 能正常迭代强转而不发生 ClassCastException")
    @SuppressWarnings("unchecked")
    void testListChallengesWithCachedValueRoundTrip() {
        CtfCompetitionChallengeVO cachedVo = CtfCompetitionChallengeVO.builder()
                .id(10L)
                .name("签到题")
                .currentPoints(1000)
                .isSolvedByMyTeam(false)
                .build();

        List<CtfCompetitionChallengeVO> baseList = List.of(cachedVo);
        CachedValue cv = CachedValue.of(baseList);
        CachedValue decoded = CachedValue.decode(cv.encode());
        assertNotNull(decoded);
        List<CtfCompetitionChallengeVO> deserializedList = (List<CtfCompetitionChallengeVO>) decoded.unwrap(null);

        when(cacheTemplate.getOrLoad(eq(CacheNames.COMPETITION_STATUS), eq("ctf:challenges:100"), any()))
                .thenReturn(deserializedList);

        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        when(solvedRepository.findByCompetitionIdAndTeamId(100L, 200L)).thenReturn(Collections.emptyList());

        List<CtfCompetitionChallengeVO> result = ctfCompetitionService.listChallenges(100L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("签到题", result.get(0).getName());
        assertFalse(result.get(0).getIsSolvedByMyTeam());
    }

    @Test
    @DisplayName("验证 getChallengeDetail 走三级缓存并支持不可变派生")
    void testGetChallengeDetailCache() {
        CtfCompetitionChallengeDetailVO cachedDetail = CtfCompetitionChallengeDetailVO.builder()
                .id(10L)
                .name("签到题")
                .description("欢迎参加比赛")
                .currentPoints(1000)
                .isContainer(false)
                .isSolvedByMyTeam(false)
                .build();

        when(cacheTemplate.getOrLoad(eq(CacheNames.CTF_CHALLENGE_DETAIL), eq("comp:100:chal:10"), any()))
                .thenReturn(cachedDetail);

        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        when(solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(100L, 10L, 200L)).thenReturn(true);

        CtfCompetitionChallengeDetailVO vo = ctfCompetitionService.getChallengeDetail(100L, 10L, 1L);

        assertNotNull(vo);
        assertTrue(vo.getIsSolvedByMyTeam());
        assertFalse(cachedDetail.getIsSolvedByMyTeam(), "原缓存对象未被篡改");
    }

    @Test
    @DisplayName("验证 startInstance 具备同队同题防并发多开分布式锁保护")
    @SuppressWarnings("unchecked")
    void testStartInstanceDistributedLock() {
        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        when(challengeRepository.findByIdWithChallenge(10L)).thenReturn(Optional.of(compChallenge));

        ChallengeInstanceResponseVO mockResponse = ChallengeInstanceResponseVO.builder()
                .challengeId(1L)
                .accessUrl("http://127.0.0.1:30001")
                .build();

        // 模拟分布式锁执行成功
        when(redisOps.withLock(eq("volur:lock:container:start:100:10:200"), any(Duration.class), any(Duration.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());

        when(containerInstanceService.startInstance(eq(1L), eq(1L), anyLong(), eq(200L), any()))
                .thenReturn(mockResponse);

        ChallengeInstanceResponseVO result = ctfCompetitionService.startInstance(100L, 10L, 1L);
        assertNotNull(result);
        assertEquals("http://127.0.0.1:30001", result.getAccessUrl());
    }

    @Test
    @DisplayName("验证 startInstance 锁竞争冲突时抛出 409 业务异常")
    @SuppressWarnings("unchecked")
    void testStartInstanceLockConflict() {
        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        when(challengeRepository.findByIdWithChallenge(10L)).thenReturn(Optional.of(compChallenge));

        when(redisOps.withLock(anyString(), any(Duration.class), any(Duration.class), any(Supplier.class)))
                .thenThrow(new IllegalStateException("获取分布式锁超时"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                ctfCompetitionService.startInstance(100L, 10L, 1L)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("正在启动或排队中"));
    }

    @Test
    @DisplayName("验证 submitFlag 正确解出时主动驱逐题目列表、题目详情与天梯榜物化缓存")
    void testSubmitFlagEvictsCaches() {
        when(memberRepository.findMemberWithTeam(100L, 1L)).thenReturn(Optional.of(member));
        when(challengeRepository.findByIdWithChallenge(10L)).thenReturn(Optional.of(compChallenge));
        when(solvedRepository.existsByCompetitionIdAndCompetitionChallengeIdAndTeamId(100L, 10L, 200L)).thenReturn(false);
        when(solvedRepository.findByCompetitionChallengeIdOrderBySolvedAtAsc(10L)).thenReturn(Collections.emptyList());

        CtfFlagSubmitRequestDTO req = new CtfFlagSubmitRequestDTO();
        req.setFlag("flag{test_success}");

        boolean isCorrect = ctfCompetitionService.submitFlag(100L, 10L, 1L, req, "127.0.0.1");

        assertTrue(isCorrect);
        verify(cacheTemplate).evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:100");
        verify(cacheTemplate).evict(CacheNames.CTF_CHALLENGE_DETAIL, "comp:100:chal:10");
        verify(cacheTemplate).evict(CacheNames.COMPETITION_STATUS, "scoreboard:ctf:100");
        verify(cacheTemplate).evict(CacheNames.COMPETITION_STATUS, "scoreboard:composite:100");
        verify(realtimePushService).broadcastAll(eq("competition_event"), any());
    }

    @Test
    @DisplayName("验证非参赛人员且非管理员访问赛题列表抛出 403 异常")
    void testListChallengesForbiddenWhenNotParticipating() {
        when(memberRepository.findMemberWithTeam(100L, 999L)).thenReturn(Optional.empty());
        User nonAdmin = User.builder().id(999L).role(UserRole.USER).build();
        when(userRepository.findById(999L)).thenReturn(Optional.of(nonAdmin));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                ctfCompetitionService.listChallenges(100L, 999L)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("验证管理员即便未加入战队也可访问赛题列表")
    void testListChallengesAllowedForAdminWithoutTeam() {
        when(memberRepository.findMemberWithTeam(100L, 888L)).thenReturn(Optional.empty());
        User admin = User.builder().id(888L).role(UserRole.ADMIN).build();
        when(userRepository.findById(888L)).thenReturn(Optional.of(admin));

        CtfCompetitionChallengeVO cachedVo = CtfCompetitionChallengeVO.builder()
                .id(10L)
                .name("签到题")
                .currentPoints(1000)
                .build();
        when(cacheTemplate.getOrLoad(eq(CacheNames.COMPETITION_STATUS), eq("ctf:challenges:100"), any()))
                .thenReturn(List.of(cachedVo));

        List<CtfCompetitionChallengeVO> result = ctfCompetitionService.listChallenges(100L, 888L);
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
