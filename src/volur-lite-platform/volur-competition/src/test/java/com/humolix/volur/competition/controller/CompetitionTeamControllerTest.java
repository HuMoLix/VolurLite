package com.humolix.volur.competition.controller;

import com.humolix.volur.competition.controller.v1.CompetitionTeamController;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.framework.security.UserIdentity;
import com.humolix.volur.framework.security.resolver.CurrentUserIdArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompetitionTeamControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompetitionTeamService teamService;

    @InjectMocks
    private CompetitionTeamController competitionTeamController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.standaloneSetup(competitionTeamController)
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("已登录用户访问 GET /api/v1/competitions/{compId}/teams/me：放行并透传 userId")
    void testGetMyTeam_whenAuthenticated_shouldReturnOk() throws Exception {
        Long competitionId = 1L;
        Long userId = 1001L;

        UserIdentity identity = new UserIdentity() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getUsername() {
                return "test_user";
            }
        };

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(identity, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MyTeamInfoVO myTeamVO = MyTeamInfoVO.builder()
                .teamId(10L)
                .teamName("Team Alpha")
                .build();

        when(teamService.getMyTeam(competitionId, userId)).thenReturn(myTeamVO);

        mockMvc.perform(get("/api/v1/competitions/{competitionId}/teams/me", competitionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("获取我的队伍信息成功"))
                .andExpect(jsonPath("$.data.teamName").value("Team Alpha"));
    }

    @Test
    @DisplayName("未登录匿名用户访问 GET /api/v1/competitions/{compId}/teams/me：被拦截并抛出 401 异常")
    void testGetMyTeam_whenUnauthenticated_shouldBeRejected() {
        Long competitionId = 1L;

        // 无任何认证上下文，直接请求被 CurrentUserIdArgumentResolver 拦截抛出 UNAUTHORIZED BusinessException
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/v1/competitions/{competitionId}/teams/me", competitionId)
                    .contentType(MediaType.APPLICATION_JSON));
        });
    }

}
