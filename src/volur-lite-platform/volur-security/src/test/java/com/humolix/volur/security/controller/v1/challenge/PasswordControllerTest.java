package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.security.domain.dto.PasswordChallengeRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.service.PasswordService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;

class PasswordControllerTest {

    private PasswordService passwordService;
    private PasswordController passwordController;

    @BeforeEach
    void setUp() {
        passwordService = Mockito.mock(PasswordService.class);
        passwordController = new PasswordController(passwordService);
    }

    @Test
    void verify_whenValidRequest_shouldCallPasswordServiceAndReturnSuccess() {
        PasswordChallengeRequestDTO request = new PasswordChallengeRequestDTO();
        request.setDsh("S-100200300:1790000000");
        request.setTl("ADG-PWD_TICKET");
        request.setPassword("correct_password");

        FlowResponseVO expectedResponse = FlowResponseVO.builder()
                .dsh("S-100200300:1790000000")
                .tl("ADG-AUTH_TICKET")
                .cid(2)
                .nextStep("authenticate")
                .status("CHALLENGE")
                .identifier("testuser")
                .build();

        given(passwordService.verifyPassword(request)).willReturn(expectedResponse);

        ApiResponseEntity<FlowResponseVO> responseEntity = passwordController.verify(request);

        Assertions.assertNotNull(responseEntity);
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertTrue(responseEntity.getBody().success());
        Assertions.assertEquals("密码安全核验通过", responseEntity.getBody().message());
        Assertions.assertEquals(expectedResponse, responseEntity.getBody().data());
    }

}
