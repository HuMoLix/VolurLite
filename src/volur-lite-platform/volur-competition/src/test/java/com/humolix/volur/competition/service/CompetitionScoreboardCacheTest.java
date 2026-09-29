package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.vo.scoreboard.CompositeScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfScoreboardVO;
import com.humolix.volur.competition.domain.vo.scoreboard.CtfTeamRankVO;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.awd.AwdCompetitionLogRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.service.impl.CompetitionScoreboardServiceImpl;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionScoreboardCacheTest {

    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private CompetitionConfigRepository configRepository;
    @Mock
    private CompetitionTeamRepository teamRepository;
    @Mock
    private CtfCompetitionSolvedRepository ctfSolvedRepository;
    @Mock
    private TheoryCompetitionAnswerRepository theoryAnswerRepository;
    @Mock
    private AwdCompetitionLogRepository awdLogRepository;
    @Mock
    private VolurCacheTemplate cacheTemplate;

    @InjectMocks
    private CompetitionScoreboardServiceImpl scoreboardService;

    @Test
    @DisplayName("验证 getCtfScoreboard 接入三级物化缓存，未命中时回源计算")
    @SuppressWarnings("unchecked")
    void testGetCtfScoreboardMaterializedCache() throws Exception {
        CtfScoreboardVO expectedVO = CtfScoreboardVO.builder()
                .competitionId(100L)
                .isFrozen(false)
                .rankings(List.of(CtfTeamRankVO.builder().rank(1).teamId(200L).teamName("战狼").score(1000).build()))
                .build();

        // 模拟缓存命中
        when(cacheTemplate.getOrLoad(eq(CacheNames.COMPETITION_STATUS), eq("scoreboard:ctf:100"), any(Callable.class)))
                .thenReturn(expectedVO);

        CtfScoreboardVO vo = scoreboardService.getCtfScoreboard(100L);

        assertNotNull(vo);
        assertEquals(100L, vo.getCompetitionId());
        assertEquals(1, vo.getRankings().size());
        assertEquals("战狼", vo.getRankings().get(0).getTeamName());

        // 验证没有直接调用底层的多表关联查询
        verify(teamRepository, never()).findByCompetitionIdAndStatus(anyLong(), any());
        verify(ctfSolvedRepository, never()).findAllSolvesWithDetails(anyLong());
    }

    @Test
    @DisplayName("验证 getCompositeScoreboard 接入三级物化缓存")
    @SuppressWarnings("unchecked")
    void testGetCompositeScoreboardMaterializedCache() throws Exception {
        CompositeScoreboardVO expectedVO = CompositeScoreboardVO.builder()
                .competitionId(100L)
                .competitionTitle("2026 迎新赛")
                .isFrozen(false)
                .rankings(Collections.emptyList())
                .build();

        when(cacheTemplate.getOrLoad(eq(CacheNames.COMPETITION_STATUS), eq("scoreboard:composite:100"), any(Callable.class)))
                .thenReturn(expectedVO);

        CompositeScoreboardVO vo = scoreboardService.getCompositeScoreboard(100L);

        assertNotNull(vo);
        assertEquals("2026 迎新赛", vo.getCompetitionTitle());
        verify(competitionRepository, never()).findById(anyLong());
    }
}
