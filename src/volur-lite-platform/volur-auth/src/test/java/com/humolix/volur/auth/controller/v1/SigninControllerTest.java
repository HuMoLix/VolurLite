package com.humolix.volur.auth.controller.v1;

import com.humolix.volur.auth.domain.dto.signin.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserItemVO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.auth.service.SigninService;
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
class SigninControllerTest {

    @Mock
    private SigninService signinService;

    private SigninController signinController;

    @BeforeEach
    void setUp() {
        signinController = new SigninController(signinService);
    }

    @Test
    void getAccounts_shouldReturnOkResponseEntityWithAccounts() {
        AccountChooserProbeRequestDTO request = new AccountChooserProbeRequestDTO(List.of("alice"));
        AccountChooserProbeResponseVO vo = AccountChooserProbeResponseVO.builder()
                .accounts(List.of(
                        AccountChooserItemVO.builder()
                                .identifier("alice")
                                .username("alice")
                                .sessionStatus("ACTIVE")
                                .build()
                ))
                .build();
        given(signinService.getAccounts(request)).willReturn(vo);

        ApiResponseEntity<AccountChooserProbeResponseVO> response = signinController.getAccounts(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("获取账号列表成功", response.getBody().message());
        Assertions.assertEquals(vo, response.getBody().data());
    }
}
