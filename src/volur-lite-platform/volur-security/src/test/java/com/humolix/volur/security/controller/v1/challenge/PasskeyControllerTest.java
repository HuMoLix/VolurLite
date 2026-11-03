package com.humolix.volur.security.controller.v1.challenge;

import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.security.domain.dto.PasskeyAssertionVerifyRequestDTO;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyCredentialVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.service.PasskeyService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PasskeyControllerTest {

    private PasskeyService passkeyService;
    private PasskeyController controller;

    @BeforeEach
    void setUp() {
        passkeyService = Mockito.mock(PasskeyService.class);
        controller = new PasskeyController(passkeyService);
    }

    @Test
    void listCredentials_shouldReturnOk() {
        PasskeyCredentialVO vo = PasskeyCredentialVO.builder()
                .credentialId("test-id")
                .label("My Key")
                .build();
        given(passkeyService.listCredentials(1001L)).willReturn(List.of(vo));

        ApiResponseEntity<List<PasskeyCredentialVO>> response = controller.listCredentials(1001L);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, response.getBody().data().size());
        Assertions.assertEquals("test-id", response.getBody().data().get(0).getCredentialId());
        Assertions.assertEquals("获取通行密钥凭据列表成功", response.getBody().message());
    }

    @Test
    void deleteCredential_shouldReturnSuccess() {
        ApiResponseEntity<Void> response = controller.deleteCredential(1001L, "test-id");

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("通行密钥凭据解绑成功", response.getBody().message());
        verify(passkeyService).deleteCredential(1001L, "test-id");
    }

    @Test
    void getSupportedAuthenticators_shouldReturnOk() {
        PasskeyAuthenticatorOptionVO opt = PasskeyAuthenticatorOptionVO.builder().key("windows_hello").build();
        given(passkeyService.getSupportedAuthenticators()).willReturn(List.of(opt));

        ApiResponseEntity<List<PasskeyAuthenticatorOptionVO>> response = controller.getSupportedAuthenticators();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, response.getBody().data().size());
    }

    @Test
    void verifyPasskeyAssertion_shouldReturnOk() {
        PasskeyAssertionVerifyRequestDTO req = PasskeyAssertionVerifyRequestDTO.builder().dsh("DSH").tl("TL").build();
        FlowResponseVO vo = FlowResponseVO.builder().status("SUCCESS").build();
        given(passkeyService.verifyPasskeyAssertion(req)).willReturn(vo);

        ApiResponseEntity<FlowResponseVO> response = controller.verifyPasskeyAssertion(req);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("通行密钥核验成功", response.getBody().message());
    }

}
