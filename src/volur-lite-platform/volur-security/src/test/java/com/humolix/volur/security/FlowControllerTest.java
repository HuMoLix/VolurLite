package com.humolix.volur.security;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.security.controller.v1.FlowController;
import com.humolix.volur.security.domain.vo.FlowInitVO;
import com.humolix.volur.security.service.FlowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FlowControllerTest {

    @Mock
    private FlowService flowService;

    @InjectMocks
    private FlowController flowController;

    @Test
    @DisplayName("initSess 成功调用并返回 VLIF 会话初始化数据")
    void initSess_shouldReturnFlowInitVO() {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "e10adc3949ba59abbe56e057f20f883e";
        Long t = 1790527266429L;

        FlowInitVO mockVO = FlowInitVO.builder()
                .dsh("S-969965009:1790527266429740")
                .flowName(flowName)
                .flowEntry(flowEntry)
                .cid(0)
                .fp(fp)
                .serverTime(System.currentTimeMillis())
                .build();

        given(flowService.initSess(flowName, flowEntry, fp, t, null)).willReturn(mockVO);

        ApiResponse<FlowInitVO> response = flowController.initSess(flowName, flowEntry, fp, t, null);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getDsh()).isEqualTo("S-969965009:1790527266429740");
        assertThat(response.data().getFlowName()).isEqualTo("VlifWebLogin");
        assertThat(response.data().getFlowEntry()).isEqualTo("WebServiceLogin");
        assertThat(response.data().getFp()).isEqualTo(fp);
    }
}
