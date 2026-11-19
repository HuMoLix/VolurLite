package com.humolix.volur.auth.service;

import com.humolix.volur.auth.domain.dto.signin.*;
import com.humolix.volur.auth.domain.vo.AccountChooserResponseVO;
import com.humolix.volur.auth.domain.vo.SignInFlowResponseVO;

public interface SignInService {

    AccountChooserResponseVO getAccounts(AccountChooserProbeRequestDTO request);

    default AccountChooserResponseVO getAccounts() {
        return getAccounts(new AccountChooserProbeRequestDTO());
    }

    SignInFlowResponseVO selectAccount(AccountChooserSelectRequestDTO request);

    void removeAccount(AccountChooserRemoveRequestDTO request);

    SignInFlowResponseVO identifier(SignInIdentifierRequestDTO request);

    SignInFlowResponseVO authenticate(SignInAuthenticateRequestDTO request);

}
