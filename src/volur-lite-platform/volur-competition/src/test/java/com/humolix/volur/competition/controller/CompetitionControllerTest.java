package com.humolix.volur.competition.controller;

import com.humolix.volur.competition.controller.v1.CompetitionController;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.competition.service.CompetitionService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.api.SliceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompetitionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompetitionService competitionService;

    @InjectMocks
    private CompetitionController competitionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(competitionController).build();
    }

    @Test
    @DisplayName("验证 GET /api/v1/competitions 接口路由正常")
    void testListCompetitionsRoute() throws Exception {
        CompetitionSummaryVO summaryVO = CompetitionSummaryVO.builder()
                .id(1L)
                .title("Volur 2026 迎新赛")
                .slug("volur-2026-welcome")
                .build();

        PageResponse<CompetitionSummaryVO> pageResponse = PageResponse.of(List.of(summaryVO), 1L, 1, 10);
        when(competitionService.listCompetitions(anyInt(), anyInt(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/competitions")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("获取竞赛列表成功"))
                .andExpect(jsonPath("$.data.list[0].title").value("Volur 2026 迎新赛"));
    }

    @Test
    @DisplayName("验证 GET /api/v1/competitions/slice 接口切片路由正常")
    void testListCompetitionSliceRoute() throws Exception {
        CompetitionSummaryVO summaryVO = CompetitionSummaryVO.builder()
                .id(1L)
                .title("Volur 2026 迎新赛")
                .slug("volur-2026-welcome")
                .build();

        SliceResponse<CompetitionSummaryVO> sliceResponse = SliceResponse.of(List.of(summaryVO), false, false, 1, 5);
        when(competitionService.listCompetitionSlice(anyInt(), anyInt(), any(), any())).thenReturn(sliceResponse);

        mockMvc.perform(get("/api/v1/competitions/slice")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("获取竞赛切片列表成功"))
                .andExpect(jsonPath("$.data.list[0].title").value("Volur 2026 迎新赛"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    @Test
    @DisplayName("验证 GET /api/v1/competitions/{id}/announcements 接口路由正常")
    void testListAnnouncementsRoute() throws Exception {
        CompetitionAnnouncementVO announcementVO = CompetitionAnnouncementVO.builder()
                .id(10L)
                .title("赛事开始通知")
                .content("比赛将于明日准时开启")
                .isPinned(true)
                .build();

        when(competitionService.listAnnouncements(eq(1L), any())).thenReturn(List.of(announcementVO));

        mockMvc.perform(get("/api/v1/competitions/1/announcements")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("获取竞赛公告成功"))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].title").value("赛事开始通知"))
                .andExpect(jsonPath("$.data[0].isPinned").value(true));
    }
}
