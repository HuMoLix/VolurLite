package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.security.domain.dto.TOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.TotpEnableRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.TotpEnableResponseVO;
import com.humolix.volur.security.domain.vo.TotpSetupVO;
import com.humolix.volur.security.service.TotpService;
import com.humolix.volur.security.domain.vo.TotpStatusVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class TOTPChallengeControllerTest {

    private TotpService totpService;
    private TOTPChallengeController controller;

    @BeforeEach
    void setUp() {
        totpService = Mockito.mock(TotpService.class);
        controller = new TOTPChallengeController(totpService);
    }

    @Test
    void setupTotp_shouldReturnOk() {
        String dsh = "DSH_1";
        String tl = "TL_1";
        TotpSetupVO mockVo = TotpSetupVO.builder().account("alice").issuer("Volur").secret("SECRET").build();
        given(totpService.setupTotp(dsh, tl)).willReturn(mockVo);

        ApiResponseEntity<TotpSetupVO> response = controller.setupTotp(dsh, tl);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(mockVo, response.getBody().data());
        Assertions.assertEquals("获取动态身份验证器配置成功", response.getBody().message());
    }

    @Test
    void enableTotp_shouldReturnOk() {
        TotpEnableRequestDTO req = TotpEnableRequestDTO.builder().dsh("DSH_1").tl("TL_1").code("123456").build();
        TotpEnableResponseVO mockVo = TotpEnableResponseVO.builder().success(true).message("动态身份验证器绑定成功").build();
        given(totpService.enableTotp(req)).willReturn(mockVo);

        ApiResponseEntity<TotpEnableResponseVO> response = controller.enableTotp(req);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(mockVo, response.getBody().data());
        Assertions.assertEquals("动态身份验证器绑定成功", response.getBody().message());
    }

    @Test
    void verifyTotp_shouldReturnOk() {
        TOTPChallengeRequestDTO req = TOTPChallengeRequestDTO.builder().dsh("DSH_1").tl("TL_1").code("123456").build();
        FlowResponseVO mockVo = FlowResponseVO.builder().dsh("DSH_1").nextStep("authenticate").status("CHALLENGE").build();
        given(totpService.verifyTotp(req)).willReturn(mockVo);

        ApiResponseEntity<FlowResponseVO> response = controller.verifyTotp(req);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(mockVo, response.getBody().data());
        Assertions.assertEquals("安全口令核验通过", response.getBody().message());
    }

    @Test
    void getStatus_shouldReturnOk() {
        TotpStatusVO mockVo = TotpStatusVO.builder().enabled(true).build();
        given(totpService.getTotpStatus(1001L)).willReturn(mockVo);

        ApiResponseEntity<TotpStatusVO> response = controller.getStatus(1001L);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(mockVo, response.getBody().data());
        Assertions.assertEquals("获取动态身份验证器状态成功", response.getBody().message());
    }

    @Test
    void disable_shouldReturnSuccess() {
        ApiResponseEntity<Void> response = controller.disable(1001L);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("动态身份验证器解绑成功", response.getBody().message());
        verify(totpService).disableTotp(1001L);
    }

}
