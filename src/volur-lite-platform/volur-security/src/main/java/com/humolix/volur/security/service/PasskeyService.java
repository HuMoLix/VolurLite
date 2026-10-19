package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.PasskeyAssertionVerifyRequestDTO;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.domain.vo.PasskeyCredentialVO;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

import java.util.List;

public interface PasskeyService {

    PublicKeyCredentialCreationOptions getCreationOptions(String dsh, String tl, String authenticatorType);

    default PublicKeyCredentialCreationOptions getCreationOptions(String dsh, String tl) {
        return getCreationOptions(dsh, tl, null);
    }

    List<PasskeyAuthenticatorOptionVO> getSupportedAuthenticators();

    PasskeyRegisterResponseVO registerPasskey(String dsh, String tl, PasskeyRegisterRequestDTO request);

    PublicKeyCredentialRequestOptions getAssertionOptions(String dsh, String tl);

    FlowResponseVO verifyPasskeyAssertion(PasskeyAssertionVerifyRequestDTO request);

    boolean hasPasskey(Long userId);

    List<PasskeyCredentialVO> listCredentials(Long userId);

    void deleteCredential(Long userId, String credentialId);
}
