package com.humolix.volur.security;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
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
    @DisplayName("Successfully initialize authentication flow session without dsh")
    void testInitSess_Success() {
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String fp = "4f8a9e2d";
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

        ApiResponseEntity<FlowInitVO> entity = flowController.initSess(flowName, flowEntry, fp, t, null);

        assertThat(entity).isNotNull();
        ApiResponse<FlowInitVO> response = entity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getDsh()).isEqualTo("S-969965009:1790527266429740");
        assertThat(response.data().getFlowName()).isEqualTo("VlifWebLogin");
        assertThat(response.data().getFlowEntry()).isEqualTo("WebServiceLogin");
        assertThat(response.data().getFp()).isEqualTo(fp);
    }
}
