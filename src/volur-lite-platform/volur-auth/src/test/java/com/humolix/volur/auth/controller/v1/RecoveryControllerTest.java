package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoveryEmailRequestDTO;
import com.humolix.volur.auth.domain.dto.recovery.UsernameRecoverySelectRequestDTO;
import com.humolix.volur.auth.domain.vo.RecoveryFlowResponseVO;
import com.humolix.volur.auth.domain.vo.RecoveryMethodVO;
import com.humolix.volur.auth.domain.vo.UsernameRecoveryOptionsVO;
import com.humolix.volur.auth.service.RecoveryService;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecoveryControllerTest {

    @Mock
    private RecoveryService recoveryService;

    private RecoveryController recoveryController;

    @BeforeEach
    void setUp() {
        recoveryController = new RecoveryController(recoveryService);
    }

    @Test
    void getUsernameRecoveryOptions_shouldReturnOkResponseEntity() {
        UsernameRecoveryOptionsVO vo = UsernameRecoveryOptionsVO.builder()
                .methods(List.of(RecoveryMethodVO.builder().method("EMAIL").name("电子邮箱").build()))
                .build();
        given(recoveryService.getUsernameRecoveryOptions()).willReturn(vo);

        ApiResponseEntity<UsernameRecoveryOptionsVO> response = recoveryController.getUsernameRecoveryOptions();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(vo, response.getBody().data());
    }

    @Test
    void selectRecoveryMethod_shouldReturnOkResponseEntityWithRecoveryFlowResponseVO() {
        UsernameRecoverySelectRequestDTO request = UsernameRecoverySelectRequestDTO.builder()
                .dsh("DSH_123")
                .tl("TL_1")
                .cid(0)
                .method("EMAIL")
                .build();
        RecoveryFlowResponseVO vo = RecoveryFlowResponseVO.builder()
                .dsh("DSH_123")
                .tl("TL_2")
                .cid(1)
                .nextStep("recaptcha")
                .status("CHALLENGE")
                .build();
        given(recoveryService.selectRecoveryMethod(request)).willReturn(vo);

        ApiResponseEntity<RecoveryFlowResponseVO> response = recoveryController.selectRecoveryMethod(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(vo, response.getBody().data());
    }

    @Test
    void submitRecoveryEmail_shouldReturnOkResponseEntityWithRecoveryFlowResponseVO() {
        UsernameRecoveryEmailRequestDTO request = UsernameRecoveryEmailRequestDTO.builder()
                .dsh("DSH_123")
                .email("user@volur.com")
                .tl("TL_2")
                .cid(2)
                .build();
        RecoveryFlowResponseVO vo = RecoveryFlowResponseVO.builder()
                .dsh("DSH_123")
                .status("SUCCESS")
                .build();
        given(recoveryService.submitRecoveryEmail(request)).willReturn(vo);

        ApiResponseEntity<RecoveryFlowResponseVO> response = recoveryController.submitRecoveryEmail(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(vo, response.getBody().data());
    }

}
