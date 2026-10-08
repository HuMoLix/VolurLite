package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.signin.AccountChooserProbeRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserRemoveRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.AccountChooserSelectRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInAuthenticateRequestDTO;
import com.humolix.volur.auth.domain.dto.signin.SignInIdentifierRequestDTO;
import com.humolix.volur.auth.domain.vo.AccountChooserProbeResponseVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;

public interface SigninService {

    AccountChooserProbeResponseVO probeAccounts(AccountChooserProbeRequestDTO request);

    SignInFlowResponseVO selectAccount(AccountChooserSelectRequestDTO request);

    void removeAccount(AccountChooserRemoveRequestDTO request);

    SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request);

    SignInFlowResponseVO authenticate(SignInAuthenticateRequestDTO request);

}
