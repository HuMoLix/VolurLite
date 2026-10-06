package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.dto.AccountChooserRemoveRequestDTO;
import com.humolix.volur.auth.domain.dto.AccountChooserSelectRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInAuthenticateRequestDTO;
import com.humolix.volur.auth.domain.dto.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.security.domain.vo.SignInFlowResponseVO;

public interface SigninService {

    AccountChooserProbeResponseVO probeAccounts(AccountChooserProbeRequestDTO request);

    SignInFlowResponseVO selectAccount(AccountChooserSelectRequestDTO request);

    void removeAccount(AccountChooserRemoveRequestDTO request);

    SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request);

    SignInFlowResponseVO authenticate(SignInAuthenticateRequestDTO request);

}
